package com.finflow.commerce.entity;

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
import java.util.List;
import java.util.UUID;

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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected CommerceOrder() {
    }

    public CommerceOrder(Long organisationId, Long customerUserId, String currencyCode) {
        this.organisationId = organisationId;
        this.customerUserId = customerUserId;
        this.orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.status = OrderStatus.CREATED;
        this.totalAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.currencyCode = currencyCode;
        this.createdAt = Instant.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        totalAmount = totalAmount.add(item.lineTotal());
    }

    public void applyDiscount(String couponCode, BigDecimal discountedTotal) {
        BigDecimal before = this.totalAmount;
        this.couponCode = couponCode;
        this.discountAmount = before.subtract(discountedTotal);
        this.totalAmount = discountedTotal;
    }

    public void transitionTo(OrderStatus next) {
        this.status = next;
    }

    public void recordPayment(Long walletId, Long transactionId) {
        this.paymentWalletId = walletId;
        this.paymentTransactionId = transactionId;
    }

    public void setShippingAddress(ShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
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

    public List<OrderItem> getItems() {
        return items;
    }
}
