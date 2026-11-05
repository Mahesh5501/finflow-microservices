package com.finflow.commerce.service;

import com.finflow.commerce.dto.OrderResponse;
import com.finflow.commerce.entity.CommerceOrder;
import com.finflow.commerce.entity.OrderItem;
import com.finflow.commerce.entity.OrderStatus;
import com.finflow.commerce.entity.Product;
import com.finflow.commerce.event.CommerceEventPublisher;
import com.finflow.commerce.event.CommerceEventType;
import com.finflow.commerce.event.CommerceEvents;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.repository.CommerceOrderRepository;
import com.finflow.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCancelService {

    private final CommerceOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CommerceEventPublisher eventPublisher;

    public OrderCancelService(
            CommerceOrderRepository orderRepository,
            ProductRepository productRepository,
            CommerceEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderResponse finalizeCancel(Long orderId) {
        CommerceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return OrderResponse.from(order);
        }
        if (order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            for (OrderItem item : order.getItems()) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "Product missing during stock release"));
                product.releaseStock(item.getQuantity());
            }
        }
        order.transitionTo(OrderStatus.CANCELLED);
        CommerceOrder saved = orderRepository.save(order);
        saved.getItems().size();
        eventPublisher.publish(CommerceEvents.of(saved, CommerceEventType.ORDER_CANCELLED));
        return OrderResponse.from(saved);
    }
}
