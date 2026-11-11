package com.finflow.commerce.order.domain;

import com.finflow.commerce.exception.ApiException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Order domain aggregate persisted by CommerceOrderRepository. Placement adds
 * immutable product/price snapshots, settlement records wallet references, and facade,
 * cancellation, expiry and return use cases read its state. It depends on OrderItem,
 * ShippingAddress, OrderStatus and ApiException, with no Spring service/HTTP dependency.
 * addItem/applyDiscount/transitionTo centralize invariants for every caller.
 *
 * @Entity makes this a managed JPA type; @Table retains orders, and @Id/IDENTITY use
 * the existing auto-increment key. Without explicit table/column names implicit naming
 * may change the schema contract. @Embedded keeps address columns on the order rather
 * than in another table. @OneToMany cascade/orphan removal owns item persistence;
 * without cascade the caller must save every child explicitly. Lazy items must be
 * mapped in a transaction, not serialized by Jackson. This entity currently has no
 * @Version; product versioning does not protect concurrent order status updates.
 */
@Entity
@Table(name = "orders")
public class CommerceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    @Column(name = "order_number", nullable = false, unique = true, length = 40)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 40)
    private String couponCode;

    @Embedded
    private ShippingAddress shippingAddress;

    @Column(name = "payment_wallet_id")
    private Long paymentWalletId;

    @Column(name = "payment_transaction_id")
    private Long paymentTransactionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Required by JPA.
     */
    protected CommerceOrder() {
    }

    public CommerceOrder(
            Long organisationId,
            Long customerUserId,
            String currencyCode
    ) {
        if (organisationId == null || organisationId <= 0) {
            throw ApiException.badRequest("Organisation ID must be positive");
        }

        if (customerUserId == null || customerUserId <= 0) {
            throw ApiException.badRequest("Customer user ID must be positive");
        }

        if (currencyCode == null || !currencyCode.trim().matches("[A-Za-z]{3}")) {
            throw ApiException.badRequest("Currency code must contain exactly three letters");
        }

        this.organisationId = organisationId;
        this.customerUserId = customerUserId;
        this.orderNumber = generateOrderNumber();
        this.status = OrderStatus.CREATED;
        this.totalAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.currencyCode = currencyCode.trim().toUpperCase(Locale.ROOT);
        this.createdAt = Instant.now();
    }

    /**
     * Adds an item to the order and updates its total amount.
     */
    public void addItem(OrderItem item) {
        if (item == null) {
            throw ApiException.badRequest("Order item cannot be null");
        }

        if (item.getQuantity() <= 0) {
            throw ApiException.badRequest("Order item quantity must be positive");
        }

        BigDecimal lineTotal = item.lineTotal();

        if (lineTotal == null || lineTotal.signum() < 0) {
            throw ApiException.badRequest("Order item total cannot be negative");
        }

        items.add(item);
        item.setOrder(this);

        totalAmount = totalAmount.add(lineTotal);
    }

    /**
     * Applies a coupon discount to the current order total.
     *
     * The discounted total must be between zero and the original total.
     */
    public void applyDiscount(
            String couponCode,
            BigDecimal discountedTotal
    ) {
        if (couponCode == null || couponCode.isBlank()) {
            throw ApiException.badRequest("Coupon code cannot be blank");
        }

        if (discountedTotal == null || discountedTotal.signum() < 0) {
            throw ApiException.badRequest("Discounted total cannot be negative");
        }

        if (discountedTotal.compareTo(totalAmount) > 0) {
            throw ApiException.badRequest(
                    "Discounted total cannot exceed the original order total"
            );
        }

        this.couponCode = couponCode.trim();
        this.discountAmount = totalAmount.subtract(discountedTotal);
        this.totalAmount = discountedTotal;
    }

    /**
     * Changes the order status only when the transition is permitted.
     */
    public void transitionTo(OrderStatus next) {
        if (next == null) {
            throw ApiException.badRequest("Next order status cannot be null");
        }

        if (!status.canTransitionTo(next)) {
            throw ApiException.conflict(
                    "INVALID_ORDER_TRANSITION",
                    "Order cannot transition from " + status + " to " + next
            );
        }

        this.status = next;
    }

    /**
     * Records the wallet and transaction identifiers returned by payment.
     */
    public void recordPayment(Long walletId, Long transactionId) {
        if (walletId == null || transactionId == null) {
            throw ApiException.badRequest(
                    "Wallet ID and payment transaction ID are required"
            );
        }

        this.paymentWalletId = walletId;
        this.paymentTransactionId = transactionId;
    }

    public void setShippingAddress(ShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    private static String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase(Locale.ROOT);
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getOrganisationId() {
        return organisationId;
    }

    public Long getCustomerUserId() {
        return customerUserId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public Long getPaymentWalletId() {
        return paymentWalletId;
    }

    public Long getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Exposes a read-only view so callers cannot bypass addItem().
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
