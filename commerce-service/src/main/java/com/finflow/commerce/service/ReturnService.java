
package com.finflow.commerce.service;

import com.finflow.commerce.dto.ReturnRequestBody;
import com.finflow.commerce.dto.ReturnResponse;
import com.finflow.commerce.entity.CommerceOrder;
import com.finflow.commerce.entity.OrderItem;
import com.finflow.commerce.entity.OrderStatus;
import com.finflow.commerce.entity.Product;
import com.finflow.commerce.entity.ReturnRequest;
import com.finflow.commerce.entity.ReturnStatus;
import com.finflow.commerce.event.CommerceEventPublisher;
import com.finflow.commerce.event.CommerceEventType;
import com.finflow.commerce.event.CommerceEvents;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.repository.CommerceOrderRepository;
import com.finflow.commerce.repository.ProductRepository;
import com.finflow.commerce.repository.ReturnRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Handles post-fulfilment product returns.
 *
 * The return workflow includes:
 * 1. Requesting a return.
 * 2. Approving or rejecting the request.
 * 3. Completing an approved return.
 *
 * Completion triggers a refund and restores product stock.
 */
@Service
public class ReturnService {

    // Stores and retrieves return requests.
    private final ReturnRequestRepository returnRequestRepository;

    // Used to load the original order.
    private final CommerceOrderRepository orderRepository;

    // Used to retrieve products and restore stock.
    private final ProductRepository productRepository;

    // Reuses the existing payment refund workflow.
    private final PaymentSettlementService paymentSettlementService;

    /*
     * Allows the final database operation to be executed
     * programmatically inside a transaction.
     *
     * This is useful here because the external refund is performed
     * separately from the database finalisation.
     */
    private final TransactionTemplate transactionTemplate;

    // Publishes the return-completed domain event.
    private final CommerceEventPublisher eventPublisher;

    /*
     * Constructor injection.
     *
     * Spring provides the required dependencies.
     */
    public ReturnService(
            ReturnRequestRepository returnRequestRepository,
            CommerceOrderRepository orderRepository,
            ProductRepository productRepository,
            PaymentSettlementService paymentSettlementService,
            TransactionTemplate transactionTemplate,
            CommerceEventPublisher eventPublisher
    ) {
        this.returnRequestRepository = returnRequestRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.paymentSettlementService = paymentSettlementService;
        this.transactionTemplate = transactionTemplate;
        this.eventPublisher = eventPublisher;
    }

    /*
     * STEP 1: CUSTOMER REQUESTS A RETURN
     *
     * A return can only be requested for an order that has been fulfilled.
     */
    @Transactional
    public ReturnResponse requestReturn(
            Long orderId,
            Long callerUserId,
            List<Long> callerOrgIds,
            ReturnRequestBody body
    ) {

        // Load the order and verify organisation access.
        CommerceOrder order = requireOrder(orderId, callerOrgIds);

        // Only the customer who placed the order can request its return.
        if (!order.getCustomerUserId().equals(callerUserId)) {
            throw ApiException.forbidden(
                    "Only the customer can request a return"
            );
        }

        // The return process begins only after fulfilment.
        if (order.getStatus() != OrderStatus.FULFILLED) {
            throw ApiException.conflict(
                    "RETURN_NOT_ALLOWED",
                    "Only FULFILLED orders can be returned"
            );
        }

        // Create a new return request using the supplied reason.
        // trim() removes unnecessary leading and trailing spaces.
        ReturnRequest created = returnRequestRepository.save(
                new ReturnRequest(
                        orderId,
                        callerUserId,
                        body.reason().trim()
                )
        );

        // Convert the saved entity to an API response.
        return ReturnResponse.from(created);
    }

    /*
     * STEP 2A: APPROVE RETURN
     *
     * The organisation approves a customer's return request.
     */
    @Transactional
    public ReturnResponse approve(
            Long returnId,
            List<Long> callerOrgIds
    ) {

        // Load the return request.
        ReturnRequest request = requireReturn(returnId);

        // Verify that the caller can access the organisation
        // associated with the original order.
        requireOrder(request.getOrderId(), callerOrgIds);

        try {
            // Delegate the state transition to the domain entity.
            request.approve();

        } catch (IllegalStateException ex) {

            // Convert an invalid domain transition into an API conflict.
            throw ApiException.conflict(
                    "RETURN_STATE",
                    ex.getMessage()
            );
        }

        return ReturnResponse.from(returnRequestRepository.save(request));
    }

    /*
     * STEP 2B: REJECT RETURN
     *
     * The organisation rejects the return request.
     */
    @Transactional
    public ReturnResponse reject(
            Long returnId,
            List<Long> callerOrgIds
    ) {

        ReturnRequest request = requireReturn(returnId);

        // Verify organisation access using the original order.
        requireOrder(request.getOrderId(), callerOrgIds);

        try {
            // Apply the domain-level rejection transition.
            request.reject();

        } catch (IllegalStateException ex) {

            throw ApiException.conflict(
                    "RETURN_STATE",
                    ex.getMessage()
            );
        }

        return ReturnResponse.from(returnRequestRepository.save(request));
    }

    /*
     * STEP 3: COMPLETE APPROVED RETURN
     *
     * This method has two distinct stages:
     *
     * 1. Perform the external refund.
     * 2. Finalise the database changes in a transaction.
     *
     * External payment operations should not be assumed to participate
     * in the local database transaction.
     */
    public ReturnResponse complete(
            Long returnId,
            List<Long> callerOrgIds,
            String accessToken
    ) {

        ReturnRequest request = requireReturn(returnId);

        CommerceOrder order = requireOrder(
                request.getOrderId(),
                callerOrgIds
        );

        // Only an approved return can be completed.
        if (request.getStatus() != ReturnStatus.APPROVED) {
            throw ApiException.conflict(
                    "RETURN_STATE",
                    "Only APPROVED returns can be completed"
            );
        }

        // Attempt the refund through the existing payment service.
        paymentSettlementService.settleRefund(order, accessToken);

        /*
         * Execute stock restoration and return completion inside
         * the configured transaction.
         *
         * The callback's result becomes the value returned by execute().
         */
        ReturnResponse completed = transactionTemplate.execute(
                status -> finalizeComplete(returnId)
        );

        // Publish the event after the transaction callback returns.
        eventPublisher.publish(
                CommerceEvents.of(order, CommerceEventType.RETURN_COMPLETED)
        );

        return completed;
    }

    /*
     * INTERNAL FINALISATION
     *
     * This method is called from the TransactionTemplate callback.
     *
     * It restores the stock for each item in the original order
     * and changes the return request to COMPLETED.
     */
    private ReturnResponse finalizeComplete(Long returnId) {

        ReturnRequest request = requireReturn(returnId);

        CommerceOrder order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> ApiException.notFound(
                        "ORDER_NOT_FOUND",
                        "Order not found"
                ));

        // Iterate over every item in the original order.
        for (OrderItem item : order.getItems()) {

            // Find the corresponding product.
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> ApiException.notFound(
                            "PRODUCT_NOT_FOUND",
                            "Product missing"
                    ));

            // Restore the quantity that was returned.
            product.releaseStock(item.getQuantity());
        }

        try {
            // Move the return request to COMPLETED.
            request.complete();

        } catch (IllegalStateException ex) {

            throw ApiException.conflict(
                    "RETURN_STATE",
                    ex.getMessage()
            );
        }

        // Save the completed return request.
        return ReturnResponse.from(returnRequestRepository.save(request));
    }

    /*
     * LIST RETURNS FOR AN ORDER
     *
     * Returns are sorted by creation time, newest first.
     */
    @Transactional(readOnly = true)
    public List<ReturnResponse> listForOrder(
            Long orderId,
            List<Long> callerOrgIds
    ) {

        // Ensure the caller can access the original order.
        requireOrder(orderId, callerOrgIds);

        // Retrieve the return requests and map them to response DTOs.
        return returnRequestRepository
                .findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(ReturnResponse::from)
                .toList();
    }

    /*
     * COMMON ORDER LOOKUP AND ORGANISATION AUTHORISATION
     */
    private CommerceOrder requireOrder(
            Long orderId,
            List<Long> callerOrgIds
    ) {

        CommerceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound(
                        "ORDER_NOT_FOUND",
                        "Order not found: " + orderId
                ));

        // A caller must have access to the order's organisation.
        if (callerOrgIds == null
                || !callerOrgIds.contains(order.getOrganisationId())) {

            throw ApiException.forbidden(
                    "No access to organisation " + order.getOrganisationId()
            );
        }

        return order;
    }

    /*
     * COMMON RETURN REQUEST LOOKUP
     */
    private ReturnRequest requireReturn(Long returnId) {

        return returnRequestRepository.findById(returnId)
                .orElseThrow(() -> ApiException.notFound(
                        "RETURN_NOT_FOUND",
                        "Return not found: " + returnId
                ));
    }
}
