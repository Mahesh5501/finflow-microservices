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
 * Post-fulfilment returns. Complete → restock + wallet refund (same money path as cancel).
 */
@Service
public class ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final CommerceOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentSettlementService paymentSettlementService;
    private final TransactionTemplate transactionTemplate;
    private final CommerceEventPublisher eventPublisher;

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

    @Transactional
    public ReturnResponse requestReturn(
            Long orderId,
            Long callerUserId,
            List<Long> callerOrgIds,
            ReturnRequestBody body
    ) {
        CommerceOrder order = requireOrder(orderId, callerOrgIds);
        if (!order.getCustomerUserId().equals(callerUserId)) {
            throw ApiException.forbidden("Only the customer can request a return");
        }
        if (order.getStatus() != OrderStatus.FULFILLED) {
            throw ApiException.conflict("RETURN_NOT_ALLOWED", "Only FULFILLED orders can be returned");
        }
        ReturnRequest created = returnRequestRepository.save(
                new ReturnRequest(orderId, callerUserId, body.reason().trim())
        );
        return ReturnResponse.from(created);
    }

    @Transactional
    public ReturnResponse approve(Long returnId, List<Long> callerOrgIds) {
        ReturnRequest request = requireReturn(returnId);
        requireOrder(request.getOrderId(), callerOrgIds);
        try {
            request.approve();
        } catch (IllegalStateException ex) {
            throw ApiException.conflict("RETURN_STATE", ex.getMessage());
        }
        return ReturnResponse.from(returnRequestRepository.save(request));
    }

    @Transactional
    public ReturnResponse reject(Long returnId, List<Long> callerOrgIds) {
        ReturnRequest request = requireReturn(returnId);
        requireOrder(request.getOrderId(), callerOrgIds);
        try {
            request.reject();
        } catch (IllegalStateException ex) {
            throw ApiException.conflict("RETURN_STATE", ex.getMessage());
        }
        return ReturnResponse.from(returnRequestRepository.save(request));
    }

    public ReturnResponse complete(Long returnId, List<Long> callerOrgIds, String accessToken) {
        ReturnRequest request = requireReturn(returnId);
        CommerceOrder order = requireOrder(request.getOrderId(), callerOrgIds);
        if (request.getStatus() != ReturnStatus.APPROVED) {
            throw ApiException.conflict("RETURN_STATE", "Only APPROVED returns can be completed");
        }
        paymentSettlementService.settleRefund(order, accessToken);
        ReturnResponse completed = transactionTemplate.execute(status -> finalizeComplete(returnId));
        eventPublisher.publish(CommerceEvents.of(order, CommerceEventType.RETURN_COMPLETED));
        return completed;
    }

    private ReturnResponse finalizeComplete(Long returnId) {
        ReturnRequest request = requireReturn(returnId);
        CommerceOrder order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found"));
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "Product missing"));
            product.releaseStock(item.getQuantity());
        }
        try {
            request.complete();
        } catch (IllegalStateException ex) {
            throw ApiException.conflict("RETURN_STATE", ex.getMessage());
        }
        return ReturnResponse.from(returnRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> listForOrder(Long orderId, List<Long> callerOrgIds) {
        requireOrder(orderId, callerOrgIds);
        return returnRequestRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(ReturnResponse::from)
                .toList();
    }

    private CommerceOrder requireOrder(Long orderId, List<Long> callerOrgIds) {
        CommerceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found: " + orderId));
        if (callerOrgIds == null || !callerOrgIds.contains(order.getOrganisationId())) {
            throw ApiException.forbidden("No access to organisation " + order.getOrganisationId());
        }
        return order;
    }

    private ReturnRequest requireReturn(Long returnId) {
        return returnRequestRepository.findById(returnId)
                .orElseThrow(() -> ApiException.notFound("RETURN_NOT_FOUND", "Return not found: " + returnId));
    }
}
