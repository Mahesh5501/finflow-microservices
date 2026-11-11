package com.finflow.commerce.order.application;

import com.finflow.commerce.order.dto.OrderResponse;
import com.finflow.commerce.order.domain.CommerceOrder;
import com.finflow.commerce.order.domain.OrderItem;
import com.finflow.commerce.order.domain.OrderStatus;
import com.finflow.commerce.catalogue.domain.Product;
import com.finflow.commerce.integration.event.CommerceEventPublisher;
import com.finflow.commerce.integration.event.CommerceEventType;
import com.finflow.commerce.order.application.CommerceEvents;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.order.persistence.CommerceOrderRepository;
import com.finflow.commerce.catalogue.persistence.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the final database-side cancellation of an order.
 *
 * Responsibilities:
 * 1. Load the order.
 * 2. Check whether cancellation has already been completed.
 * 3. Release reserved stock when required.
 * 4. Transition the order to CANCELLED.
 * 5. Save the order.
 * 6. Publish an ORDER_CANCELLED event.
 *
 * Order application finalization called by CommerceService after any required
 * refund. Repositories reload order/items and restore Product stock; the event publisher
 * announces cancellation. Constructor injection makes these required collaborators
 * explicit. @Service enables discovery; @Transactional REQUIRED keeps stock restoration
 * and CANCELLED atomic. Without that transaction a failed transition could leave stock
 * released. A caller-side transaction is an alternative only without wallet HTTP.
 * An already CANCELLED order is not restocked again. Publication is currently inside
 * the database transaction and has no durable delivery guarantee.
 */
@Service
public class OrderCancelService {

    // Repository used to load and save orders.
    private final CommerceOrderRepository orderRepository;

    // Repository used to retrieve products whose stock must be released.
    private final ProductRepository productRepository;

    // Abstraction for publishing Commerce domain events.
    // The active implementation can be Kafka or logging.
    private final CommerceEventPublisher eventPublisher;

    /**
     * Constructor injection.
     *
     * Spring supplies all required dependencies.
     */
    public OrderCancelService(
            CommerceOrderRepository orderRepository,
            ProductRepository productRepository,
            CommerceEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Finalises cancellation for an order.
     *
     * The database operations in this method participate
     * in one transaction.
     */
    @Transactional
    public OrderResponse finalizeCancel(Long orderId) {

        // Load the order or return HTTP 404 through ApiException.
        CommerceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        ApiException.notFound(
                                "ORDER_NOT_FOUND",
                                "Order not found: " + orderId
                        )
                );

        // Idempotent behaviour for an already-cancelled order:
        // do not release stock a second time.
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return OrderResponse.from(order);
        }

        // If payment has already failed, the existing flow treats
        // the stock reservation as already handled elsewhere.
        //
        // For other statuses, release the quantity reserved by
        // each order item.
        if (order.getStatus() != OrderStatus.PAYMENT_FAILED) {

            // Iterate through all items belonging to the order.
            for (OrderItem item : order.getItems()) {

                // Load the product referenced by this order item.
                Product product = productRepository
                        .findById(item.getProductId())
                        .orElseThrow(() ->
                                ApiException.notFound(
                                        "PRODUCT_NOT_FOUND",
                                        "Product missing during stock release"
                                )
                        );

                // Return the reserved quantity to available stock.
                product.releaseStock(item.getQuantity());
            }
        }

        // Move the order into its CANCELLED state.
        // The entity's transition rules decide whether this transition
        // is allowed.
        order.transitionTo(OrderStatus.CANCELLED);

        // Save the changed order.
        CommerceOrder saved = orderRepository.save(order);

        // Initialise the order items before response mapping.
        //
        // This is useful when the collection is lazily loaded,
        // because OrderResponse.from() accesses saved.getItems().
        saved.getItems().size();

        // Publish the domain event describing the cancellation.
        eventPublisher.publish(
                CommerceEvents.of(
                        saved,
                        CommerceEventType.ORDER_CANCELLED
                )
        );

        // Convert the saved entity into the API response.
        return OrderResponse.from(saved);
    }
}
