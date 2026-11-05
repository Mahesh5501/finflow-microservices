package com.finflow.commerce.service;

import com.finflow.commerce.dto.CartItemRequest;
import com.finflow.commerce.dto.CartItemResponse;
import com.finflow.commerce.dto.CartResponse;
import com.finflow.commerce.dto.CheckoutRequest;
import com.finflow.commerce.dto.CreateOrderRequest;
import com.finflow.commerce.dto.OrderItemRequest;
import com.finflow.commerce.dto.OrderResponse;
import com.finflow.commerce.dto.UpdateCartItemRequest;
import com.finflow.commerce.entity.Cart;
import com.finflow.commerce.entity.CartItem;
import com.finflow.commerce.entity.Product;
import com.finflow.commerce.entity.ProductStatus;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.repository.CartRepository;
import com.finflow.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CommerceService commerceService;
    private final TransactionTemplate transactionTemplate;

    public CartService(
            CartRepository cartRepository,
            ProductRepository productRepository,
            CommerceService commerceService,
            TransactionTemplate transactionTemplate
    ) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.commerceService = commerceService;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long customerUserId, List<Long> callerOrgIds, Long organisationId) {
        requireOrgAccess(callerOrgIds, organisationId);
        Cart cart = cartRepository.findByCustomerUserIdAndOrganisationId(customerUserId, organisationId)
                .orElseGet(() -> new Cart(customerUserId, organisationId));
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(Long customerUserId, List<Long> callerOrgIds, CartItemRequest request) {
        requireOrgAccess(callerOrgIds, request.organisationId());
        Product product = requireActiveProduct(request.productId(), request.organisationId());
        if (product.getAvailableQuantity() < request.quantity()) {
            throw ApiException.conflict("INSUFFICIENT_STOCK", "Not enough stock for product " + request.productId());
        }
        Cart cart = getOrCreate(customerUserId, request.organisationId());
        cart.addQuantity(request.productId(), request.quantity());
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse upsertItem(
            Long customerUserId,
            List<Long> callerOrgIds,
            Long productId,
            UpdateCartItemRequest request
    ) {
        requireOrgAccess(callerOrgIds, request.organisationId());
        Cart cart = getOrCreate(customerUserId, request.organisationId());
        if (request.quantity() > 0) {
            Product product = requireActiveProduct(productId, request.organisationId());
            if (product.getAvailableQuantity() < request.quantity()) {
                throw ApiException.conflict("INSUFFICIENT_STOCK", "Not enough stock for product " + productId);
            }
        }
        cart.upsertItem(productId, request.quantity());
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(Long customerUserId, List<Long> callerOrgIds, Long organisationId, Long productId) {
        requireOrgAccess(callerOrgIds, organisationId);
        Cart cart = getOrCreate(customerUserId, organisationId);
        cart.removeItem(productId);
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse clear(Long customerUserId, List<Long> callerOrgIds, Long organisationId) {
        requireOrgAccess(callerOrgIds, organisationId);
        Cart cart = getOrCreate(customerUserId, organisationId);
        cart.clearItems();
        return toResponse(cartRepository.save(cart));
    }

    /** Snapshot lines in a short TX, place order, then clear cart. */
    public OrderResponse checkout(
            Long customerUserId,
            List<Long> callerOrgIds,
            String idempotencyKey,
            String accessToken,
            CheckoutRequest request
    ) {
        requireOrgAccess(callerOrgIds, request.organisationId());
        List<OrderItemRequest> lines = transactionTemplate.execute(status -> snapshotLines(customerUserId, request.organisationId()));
        OrderResponse order = commerceService.createOrder(
                customerUserId,
                callerOrgIds,
                idempotencyKey,
                accessToken,
                new CreateOrderRequest(
                        request.organisationId(),
                        lines,
                        request.shippingAddress(),
                        request.couponCode(),
                        request.paymentOutcome()
                )
        );
        transactionTemplate.executeWithoutResult(status -> {
            Cart cart = getOrCreate(customerUserId, request.organisationId());
            cart.clearItems();
            cartRepository.save(cart);
        });
        return order;
    }

    private List<OrderItemRequest> snapshotLines(Long customerUserId, Long organisationId) {
        Cart cart = cartRepository.findByCustomerUserIdAndOrganisationId(customerUserId, organisationId)
                .orElseThrow(() -> ApiException.unprocessable("CART_EMPTY", "Cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw ApiException.unprocessable("CART_EMPTY", "Cart is empty");
        }
        return cart.getItems().stream()
                .map(i -> new OrderItemRequest(i.getProductId(), i.getQuantity()))
                .toList();
    }

    private Cart getOrCreate(Long customerUserId, Long organisationId) {
        return cartRepository.findByCustomerUserIdAndOrganisationId(customerUserId, organisationId)
                .orElseGet(() -> new Cart(customerUserId, organisationId));
    }

    private Product requireActiveProduct(Long productId, Long organisationId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "Product not found: " + productId));
        if (!product.getOrganisationId().equals(organisationId)) {
            throw ApiException.unprocessable("PRODUCT_ORG_MISMATCH", "Product belongs to another organisation");
        }
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw ApiException.unprocessable("PRODUCT_INACTIVE", "Product is not active: " + productId);
        }
        return product;
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            String name = product == null ? "Unknown" : product.getName();
            BigDecimal price = product == null ? BigDecimal.ZERO : product.getUnitPrice();
            int available = product == null ? 0 : product.getAvailableQuantity();
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            lines.add(new CartItemResponse(
                    item.getProductId(), name, price, item.getQuantity(), lineTotal, available
            ));
        }
        return new CartResponse(
                cart.getCartId(),
                cart.getOrganisationId(),
                cart.getCustomerUserId(),
                subtotal,
                cart.getUpdatedAt(),
                lines
        );
    }

    private static void requireOrgAccess(List<Long> callerOrgIds, Long organisationId) {
        if (callerOrgIds == null || !callerOrgIds.contains(organisationId)) {
            throw ApiException.forbidden("No access to organisation " + organisationId);
        }
    }
}
