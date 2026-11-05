package com.finflow.commerce.service;

import com.finflow.commerce.entity.CommerceOrder;
import com.finflow.commerce.entity.OrderItem;
import com.finflow.commerce.entity.OrderStatus;
import com.finflow.commerce.entity.Product;
import com.finflow.commerce.event.CommerceEventPublisher;
import com.finflow.commerce.event.CommerceEventType;
import com.finflow.commerce.event.CommerceEvents;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.repository.CommerceOrderRepository;
import com.finflow.commerce.repository.PaymentOutboxRepository;
import com.finflow.commerce.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Releases stock for unpaid orders past the reservation TTL (PENDING_PAYMENT / ACTION_REQUIRED).
 */
@Service
public class ReservationExpiryService {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryService.class);

    private static final EnumSet<OrderStatus> RESERVING = EnumSet.of(
            OrderStatus.PENDING_PAYMENT,
            OrderStatus.ACTION_REQUIRED
    );

    private final CommerceOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final CommerceEventPublisher eventPublisher;

    public ReservationExpiryService(
            CommerceOrderRepository orderRepository,
            ProductRepository productRepository,
            PaymentOutboxRepository outboxRepository,
            CommerceEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
    }

    /** @return number of orders expired */
    @Transactional
    public int expireStaleReservations(Instant cutoff) {
        List<CommerceOrder> stale = orderRepository.findByStatusInAndCreatedAtBefore(RESERVING, cutoff);
        int count = 0;
        for (CommerceOrder order : stale) {
            expireOne(order);
            count++;
        }
        if (count > 0) {
            log.info("Expired {} unpaid order reservation(s) older than {}", count, cutoff);
        }
        return count;
    }

    private void expireOne(CommerceOrder order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "Product missing during expiry"));
            product.releaseStock(item.getQuantity());
        }
        order.transitionTo(OrderStatus.EXPIRED);
        orderRepository.save(order);

        outboxRepository.findByOrderId(order.getOrderId()).ifPresent(outbox -> {
            if (outbox.getStatus() != com.finflow.commerce.entity.OutboxStatus.DONE) {
                outbox.markFailed("RESERVATION_TTL_EXPIRED");
                outboxRepository.save(outbox);
            }
        });

        eventPublisher.publish(CommerceEvents.of(order, CommerceEventType.ORDER_EXPIRED));
    }
}
