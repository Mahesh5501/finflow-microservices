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

/**
 * Manages shopping cart operations and coordinates checkout.
 *
 * The cart stores selected product IDs and quantities.
 * Current product prices are read when constructing the cart response.
 */
@Service
public class CartService {

    // Loads and saves customer carts.
    private final CartRepository cartRepository;

    // Used to validate products and retrieve current product information.
    private final ProductRepository productRepository;

    // Checkout delegates actual order creation and payment to CommerceService.
    private final CommerceService commerceService;

    // Used to keep the cart snapshot and cart clearing in short transactions.
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

    /**
     * Returns the customer's cart.
     *
     * If no cart exists yet, an unsaved empty cart is returned.
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(
            Long customerUserId,
            List<Long> callerOrgIds,
            Long organisationId
    ) {
        // Verify that the caller belongs to the requested organisation.
        requireOrgAccess(callerOrgIds, organisationId);

        // Find an existing cart or create an in-memory empty cart.
        // orElseGet is lazy: the new Cart is created only when no cart exists.
        Cart cart = cartRepository
                .findByCustomerUserIdAndOrganisationId(
                        customerUserId,
                        organisationId
                )
                .orElseGet(() -> new Cart(customerUserId, organisationId));

        return toResponse(cart);
    }

    /**
     * Adds a quantity to an existing product line or creates a new line.
     */
    @Transactional
    public CartResponse addItem(
            Long customerUserId,
            List<Long> callerOrgIds,
            CartItemRequest request
    ) {
        requireOrgAccess(callerOrgIds, request.organisationId());

        // Product must exist, belong to this organisation and be active.
        Product product = requireActiveProduct(
                request.productId(),
                request.organisationId()
        );

        // Check whether the requested quantity is currently available.
        if (product.getAvailableQuantity() < request.quantity()) {
            throw ApiException.conflict(
                    "INSUFFICIENT_STOCK",
                    "Not enough stock for product " + request.productId()
            );
        }

        // Find or create the customer's cart.
        Cart cart = getOrCreate(
                customerUserId,
                request.organisationId()
        );

        // Add the requested quantity to the product's existing cart quantity.
        cart.addQuantity(request.productId(), request.quantity());

        // Save the changed cart and build its response.
        return toResponse(cartRepository.save(cart));
    }

    /**
     * Sets a product line to the supplied quantity.
     *
     * A non-positive quantity is handled by Cart.upsertItem().
     */
    @Transactional
    public CartResponse upsertItem(
            Long customerUserId,
            List<Long> callerOrgIds,
            Long productId,
            UpdateCartItemRequest request
    ) {
        requireOrgAccess(callerOrgIds, request.organisationId());

        Cart cart = getOrCreate(
                customerUserId,
                request.organisationId()
        );

        // Validate product and stock only when the requested quantity is positive.
        if (request.quantity() > 0) {

            Product product = requireActiveProduct(
                    productId,
                    request.organisationId()
            );

            if (product.getAvailableQuantity() < request.quantity()) {
                throw ApiException.conflict(
                        "INSUFFICIENT_STOCK",
                        "Not enough stock for product " + productId
                );
            }
        }

        // Update or remove the line according to Cart's domain behaviour.
        cart.upsertItem(productId, request.quantity());

        return toResponse(cartRepository.save(cart));
    }

    /**
     * Removes one product line from the cart.
     */
    @Transactional
    public CartResponse removeItem(
            Long customerUserId,
            List<Long> callerOrgIds,
            Long organisationId,
            Long productId
    ) {
        requireOrgAccess(callerOrgIds, organisationId);

        Cart cart = getOrCreate(customerUserId, organisationId);

        // Cart owns the operation of removing its item.
        cart.removeItem(productId);

        return toResponse(cartRepository.save(cart));
    }

    /**
     * Removes all items from the cart.
     */
    @Transactional
    public CartResponse clear(
            Long customerUserId,
            List<Long> callerOrgIds,
            Long organisationId
    ) {
        requireOrgAccess(callerOrgIds, organisationId);

        Cart cart = getOrCreate(customerUserId, organisationId);

        // Clear the cart's item collection.
        cart.clearItems();

        return toResponse(cartRepository.save(cart));
    }

    /**
     * Coordinates checkout.
     *
     * 1. Snapshot the cart lines in a short transaction.
     * 2. Create and pay for the order through CommerceService.
     * 3. Clear the cart after successful order creation/payment flow.
     */
    public OrderResponse checkout(
            Long customerUserId,
            List<Long> callerOrgIds,
            String idempotencyKey,
            String accessToken,
            CheckoutRequest request
    ) {
        requireOrgAccess(callerOrgIds, request.organisationId());

        // Capture the cart's product IDs and quantities in a short transaction.
        // This creates an independent snapshot for order placement.
        List<OrderItemRequest> lines = transactionTemplate.execute(
                status -> snapshotLines(
                        customerUserId,
                        request.organisationId()
                )
        );

        // Delegate order creation and payment to CommerceService.
        // Product prices and stock must be revalidated by the order-placement
        // workflow because the cart snapshot is not a stock reservation.
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

        // Clear the cart only after createOrder returns successfully.
        // This is a separate transaction from order placement/payment.
        transactionTemplate.executeWithoutResult(status -> {
            Cart cart = getOrCreate(
                    customerUserId,
                    request.organisationId()
            );

            cart.clearItems();

            cartRepository.save(cart);
        });

        return order;
    }

    /**
     * Creates an immutable snapshot of the cart's product IDs and quantities.
     *
     * This method is called inside TransactionTemplate's transaction.
     */
    private List<OrderItemRequest> snapshotLines(
            Long customerUserId,
            Long organisationId
    ) {
        Cart cart = cartRepository
                .findByCustomerUserIdAndOrganisationId(
                        customerUserId,
                        organisationId
                )
                .orElseThrow(() -> ApiException.unprocessable(
                        "CART_EMPTY",
                        "Cart is empty"
                ));

        // Checkout cannot proceed without at least one cart item.
        if (cart.getItems().isEmpty()) {
            throw ApiException.unprocessable(
                    "CART_EMPTY",
                    "Cart is empty"
            );
        }

        // Convert cart entities into order request lines.
        // The resulting list is not a live reference to the Cart collection.
        return cart.getItems().stream()
                .map(i -> new OrderItemRequest(
                        i.getProductId(),
                        i.getQuantity()
                ))
                .toList();
    }

    /**
     * Finds an existing cart or creates a new in-memory cart.
     */
    private Cart getOrCreate(
            Long customerUserId,
            Long organisationId
    ) {
        return cartRepository
                .findByCustomerUserIdAndOrganisationId(
                        customerUserId,
                        organisationId
                )
                .orElseGet(() -> new Cart(
                        customerUserId,
                        organisationId
                ));
    }

    /**
     * Loads a product and validates its organisation and status.
     */
    private Product requireActiveProduct(
            Long productId,
            Long organisationId
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound(
                        "PRODUCT_NOT_FOUND",
                        "Product not found: " + productId
                ));

        // Prevent adding a product belonging to another organisation.
        if (!product.getOrganisationId().equals(organisationId)) {
            throw ApiException.unprocessable(
                    "PRODUCT_ORG_MISMATCH",
                    "Product belongs to another organisation"
            );
        }

        // Inactive products cannot be added to the cart.
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw ApiException.unprocessable(
                    "PRODUCT_INACTIVE",
                    "Product is not active: " + productId
            );
        }

        return product;
    }

    /**
     * Converts a Cart entity into the API response.
     *
     * Current product names and prices are fetched while mapping each line.
     */
    private CartResponse toResponse(Cart cart) {

        List<CartItemResponse> lines = new ArrayList<>();

        // Running subtotal for the entire cart.
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {

            // Load the current product details.
            // A deleted product is represented as Unknown in this response.
            Product product = productRepository
                    .findById(item.getProductId())
                    .orElse(null);

            String name = product == null
                    ? "Unknown"
                    : product.getName();

            BigDecimal price = product == null
                    ? BigDecimal.ZERO
                    : product.getUnitPrice();

            int available = product == null
                    ? 0
                    : product.getAvailableQuantity();

            // Calculate this line's current value.
            BigDecimal lineTotal = price.multiply(
                    BigDecimal.valueOf(item.getQuantity())
            );

            // Add the line amount to the cart subtotal.
            subtotal = subtotal.add(lineTotal);

            // Build the response line.
            lines.add(new CartItemResponse(
                    item.getProductId(),
                    name,
                    price,
                    item.getQuantity(),
                    lineTotal,
                    available
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

    /**
     * Ensures the caller belongs to the requested organisation.
     */
    private static void requireOrgAccess(
            List<Long> callerOrgIds,
            Long organisationId
    ) {
        if (callerOrgIds == null
                || !callerOrgIds.contains(organisationId)) {
            throw ApiException.forbidden(
                    "No access to organisation " + organisationId
            );
        }
    }
}