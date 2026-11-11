package com.finflow.commerce.order.application;

import com.finflow.commerce.order.dto.OrderItemRequest;
import com.finflow.commerce.order.dto.ShippingAddressRequest;
import com.finflow.commerce.order.domain.CommerceOrder;
import com.finflow.commerce.coupon.application.CouponService;
import com.finflow.commerce.coupon.domain.Coupon;
import com.finflow.commerce.order.domain.IdempotencyRecord;
import com.finflow.commerce.order.domain.OrderItem;
import com.finflow.commerce.order.domain.OrderStatus;
import com.finflow.commerce.order.domain.PaymentOutbox;
import com.finflow.commerce.catalogue.domain.Product;
import com.finflow.commerce.catalogue.domain.ProductStatus;
import com.finflow.commerce.order.domain.ShippingAddress;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.payment.PaymentOutcome;
import com.finflow.commerce.order.persistence.CommerceOrderRepository;
import com.finflow.commerce.order.persistence.IdempotencyRecordRepository;
import com.finflow.commerce.order.persistence.PaymentOutboxRepository;
import com.finflow.commerce.catalogue.persistence.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Responsible for creating a new pending-payment order.
 *
 * Main responsibilities:
 * 1. Validate seller consistency and customer-scoped request replay.
 * 2. Handle idempotency and prevent duplicate checkout orders.
 * 3. Validate products and reserve stock.
 * 4. Apply an optional coupon.
 * 5. Save the order, idempotency record and payment outbox entry.
 *
 * Important:
 * This class does not directly call the external wallet service.
 * It only prepares the order and payment outbox record.
 *
 * Order application write boundary called by CommerceService. Constructor
 * dependencies persist products, orders, idempotency keys and payment work; CouponService
 * resolves a code only within the selected seller. The order domain owns snapshots,
 * totals and status transitions; Product owns stock invariants and optimistic versioning.
 *
 * @Service enables injection; @Transactional REQUIRED puts stock, order/items, replay
 * record and outbox in one local transaction. An exception rolls them back together.
 * Removing the boundary would allow a partially reserved/created checkout. A separate
 * bean is necessary: calling a transactional method on this from the same object
 * would bypass Spring proxy advice. Wallet HTTP is deliberately absent here.
 *
 * Replay compares owner, seller and a length-prefixed SHA-256 payload fingerprint.
 * A matching key returns the original order; changed payload/owner returns a conflict.
 * The database key enforces uniqueness but concurrent first requests can still race.
 * Do not describe replay as distributed exactly-once processing. Marketplace buyers
 * are not required to be seller members; every product must belong to that seller.
 */
@Service
public class OrderPlacementService {

    // Loads managed products; dirty checking persists stock with optimistic versions.
    private final ProductRepository productRepository;
    // Persists the aggregate and cascades its historical item snapshots.
    private final CommerceOrderRepository orderRepository;
    // Records key ownership and payload fingerprint for sequential checkout replay.
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    // Writes payment intent in the placement transaction, without storing a JWT.
    private final PaymentOutboxRepository outboxRepository;
    // Coupon feature owns seller-scoped lookup; Coupon owns discount arithmetic.
    private final CouponService couponService;

    /**
     * Constructor injection.
     *
     * Spring provides the required repository implementations.
     * Using final fields ensures these dependencies cannot be reassigned.
     */
    public OrderPlacementService(
            ProductRepository productRepository,
            CommerceOrderRepository orderRepository,
            IdempotencyRecordRepository idempotencyRecordRepository,
            PaymentOutboxRepository outboxRepository,
            CouponService couponService
    ) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.outboxRepository = outboxRepository;
        this.couponService = couponService;
    }

    /**
     * Creates an order in PENDING_PAYMENT status.
     *
     * @Transactional ensures that order creation, stock reservation,
     * idempotency record creation and outbox creation participate
     * in the same database transaction.
     *
     * If an operation fails before the transaction commits,
     * the database changes are rolled back together.
     */
    @Transactional
    public Long placePendingOrder(
            Long customerUserId,
            List<Long> callerOrgIds,
            String idempotencyKey,
            Long organisationId,
            List<OrderItemRequest> items,
            ShippingAddressRequest shippingAddress,
            String couponCode,
            PaymentOutcome sandboxHint
    ) {

        // Step 1: Buyers place orders on any seller; merchant staff use org-scoped APIs separately.

        // Step 2: An idempotency key is mandatory for checkout.
        requireIdempotencyKey(idempotencyKey);

        // Step 3: Generate a fingerprint of the incoming checkout request.
        // This helps identify whether a repeated key represents the same request.
        String requestHash = calculateRequestHash(
                customerUserId,
                organisationId,
                items,
                shippingAddress,
                couponCode,
                sandboxHint
        );

        // Step 4: Check whether this idempotency key has already been used.
        var existing = idempotencyRecordRepository.findByIdempotencyKey(
                idempotencyKey.trim()
        );

        if (existing.isPresent()) {

            IdempotencyRecord previousRequest = existing.get();

            // Prevent another organisation or customer from reusing this key.
            if (!previousRequest.getOrganisationId().equals(organisationId)
                    || !previousRequest.getCustomerUserId().equals(customerUserId)) {

                throw ApiException.conflict(
                        "IDEMPOTENCY_KEY_CONFLICT",
                        "Idempotency key has already been used by another customer or organisation"
                );
            }

            // The same key must not be reused for a different checkout payload.
            if (!previousRequest.getRequestHash().equals(requestHash)) {

                throw ApiException.conflict(
                        "IDEMPOTENCY_KEY_CONFLICT",
                        "Idempotency key has already been used with a different request"
                );
            }

            // Same customer + same organisation + same request:
            // return the original order instead of creating another one.
            return previousRequest.getOrderId();
        }

        // Step 5: Create a new order for this customer and organisation.
        // INR is the current currency used by this checkout flow.
        CommerceOrder order = new CommerceOrder(
                organisationId,
                customerUserId,
                "INR"
        );

        // Step 6: Convert the shipping DTO into the domain value object.
        // Shipping address is optional in this method.
        if (shippingAddress != null) {
            order.setShippingAddress(toAddress(shippingAddress));
        }

        // Step 7: Validate every requested product and reserve its stock.
        for (OrderItemRequest line : items) {

            // Load the product from the database.
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> ApiException.notFound(
                            "PRODUCT_NOT_FOUND",
                            "Product not found: " + line.productId()
                    ));

            // A customer cannot order a product belonging to another organisation.
            if (!product.getOrganisationId().equals(organisationId)) {
                throw ApiException.unprocessable(
                        "PRODUCT_ORG_MISMATCH",
                        "Product belongs to another organisation"
                );
            }

            // Only active products can be purchased.
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw ApiException.unprocessable(
                        "PRODUCT_INACTIVE",
                        "Product is not active: " + line.productId()
                );
            }

            // Reserve the requested quantity.
            // The Product entity validates whether sufficient stock exists.
            try {
                product.reserveStock(line.quantity());

            } catch (IllegalStateException ex) {

                throw ApiException.conflict(
                        "INSUFFICIENT_STOCK",
                        "Not enough stock for product " + line.productId()
                );
            }

            // Store a snapshot of product details in the order item.
            // This preserves the product name and price at checkout time.
            order.addItem(new OrderItem(
                    product.getProductId(),
                    product.getName(),
                    product.getUnitPrice(),
                    line.quantity()
            ));
        }

        // Step 8: Apply a coupon if the customer supplied one.
        if (couponCode != null && !couponCode.isBlank()) {

            Coupon coupon = couponService.findForCheckout(organisationId, couponCode);
            try {
                order.applyDiscount(coupon.getCode(), coupon.apply(order.getTotalAmount()));
            } catch (IllegalStateException ex) {
                throw ApiException.unprocessable("COUPON_INVALID", ex.getMessage());
            }
        }

        // Step 9: Move the order from CREATED to PENDING_PAYMENT.
        order.transitionTo(OrderStatus.PENDING_PAYMENT);

        // Step 10: Save the order.
        // The generated order ID becomes available after saving.
        order = orderRepository.save(order);

        // Step 11: Save the idempotency record.
        //
        // All five constructor arguments are required:
        // key, organisation, customer, order ID and request hash.
        idempotencyRecordRepository.save(new IdempotencyRecord(
                idempotencyKey.trim(),
                organisationId,
                customerUserId,
                order.getOrderId(),
                requestHash
        ));

        // Step 12: Create the payment outbox entry.
        // A separate payment-processing flow can pick this up.
        outboxRepository.save(new PaymentOutbox(
                order.getOrderId(),
                sandboxHint == null ? null : sandboxHint.name()
        ));

        // Return the newly created order ID.
        return order.getOrderId();
    }

    /**
     * Generates a SHA-256 fingerprint for the checkout request.
     *
     * The same request data produces the same hash.
     * Changing a product, quantity, shipping address, coupon,
     * customer, organisation or sandbox hint changes the hash.
     */
    private static String calculateRequestHash(
            Long customerUserId,
            Long organisationId,
            List<OrderItemRequest> items,
            ShippingAddressRequest shippingAddress,
            String couponCode,
            PaymentOutcome sandboxHint
    ) {

        StringBuilder requestData = new StringBuilder();

        // Include the customer and organisation.
        appendHashField(requestData, String.valueOf(customerUserId));
        appendHashField(requestData, String.valueOf(organisationId));

        // Include the requested products and quantities.
        if (items == null) {
            appendHashField(requestData, null);
        } else {
            appendHashField(requestData, String.valueOf(items.size()));

            for (OrderItemRequest item : items) {
                appendHashField(requestData, String.valueOf(item.productId()));
                appendHashField(requestData, String.valueOf(item.quantity()));
            }
        }

        // Include shipping address details.
        if (shippingAddress == null) {
            appendHashField(requestData, null);
        } else {
            appendHashField(requestData, shippingAddress.line1());
            appendHashField(requestData, shippingAddress.line2());
            appendHashField(requestData, shippingAddress.city());
            appendHashField(requestData, shippingAddress.state());
            appendHashField(requestData, shippingAddress.postalCode());
            appendHashField(requestData, shippingAddress.country());
        }

        // Coupon matching is case-insensitive in this application.
        // Normalize its case before generating the fingerprint.
        appendHashField(
                requestData,
                couponCode == null
                        ? null
                        : couponCode.trim().toUpperCase(Locale.ROOT)
        );

        // Include the sandbox payment hint.
        appendHashField(
                requestData,
                sandboxHint == null ? null : sandboxHint.name()
        );

        try {
            // SHA-256 generates a 256-bit (32-byte) digest.
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    requestData.toString().getBytes(StandardCharsets.UTF_8)
            );

            // Convert the 32-byte digest into a 64-character hexadecimal string.
            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException ex) {

            // SHA-256 is part of standard Java and should always be available.
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    ex
            );
        }
    }

    /**
     * Appends one field to the fingerprint input.
     *
     * Length-prefixing prevents ambiguous concatenation.
     *
     * For example:
     * ["AB", "C"] and ["A", "BC"] must produce different input strings.
     *
     * Null is represented separately from an empty string.
     */
    private static void appendHashField(
            StringBuilder builder,
            String value
    ) {

        if (value == null) {
            builder.append("-1:");
        } else {
            builder.append(value.length())
                    .append(':')
                    .append(value);
        }
    }

    /**
     * Converts the incoming shipping address DTO
     * into the ShippingAddress domain object.
     */
    private static ShippingAddress toAddress(
            ShippingAddressRequest request
    ) {

        return new ShippingAddress(
                request.line1().trim(),
                request.line2() == null ? null : request.line2().trim(),
                request.city().trim(),
                request.state() == null ? null : request.state().trim(),
                request.postalCode().trim(),
                request.country()
        );
    }

    /**
     * Ensures that the checkout request contains an idempotency key.
     */
    private static void requireIdempotencyKey(String key) {

        if (key == null || key.isBlank()) {
            throw ApiException.badRequest(
                    "Idempotency-Key header is required"
            );
        }
    }
}
