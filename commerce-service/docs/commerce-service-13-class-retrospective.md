# Commerce Service — Retrospective of the 13 Classes Covered

## How to use this guide

This guide is based on the source in the uploaded `commerce-service.zip`. It preserves the source project's package names, public method signatures, and current model. The code snippets below are **commented learning versions of the uploaded source**, not a claim that every design is production-ready. Where a correctness issue exists, it is called out explicitly after the code.

A few earlier suggestions introduced fields, repository methods, or locking methods that are not present in this uploaded source. Do not copy those suggestions blindly. Apply changes only after checking the matching service, tests, and database migration.

---

# 1. OrderStatus.java

## Purpose

`OrderStatus` represents the lifecycle state of an order. It is an enum because an order should only be in one of a finite set of known states.

```java
package com.finflow.commerce.entity;

/**
 * Represents the business lifecycle of an order.
 * Keep payment failure, customer action, cancellation, and expiry distinct:
 * they mean different things and require different operational handling.
 */
public enum OrderStatus {
    CREATED,             // Order object created; checkout has not yet advanced it.
    PENDING_PAYMENT,     // Stock is reserved and payment is expected.
    CONFIRMED,           // Payment was approved and the order is accepted.
    PAYMENT_FAILED,      // Payment was definitively declined/failed.
    ACTION_REQUIRED,     // Payment outcome is uncertain; reconciliation/action is needed.
    CANCELLED,           // Order was explicitly cancelled.
    EXPIRED,             // Payment/reservation window elapsed.
    FULFILMENT_PENDING,  // Order is waiting for fulfilment processing.
    FULFILLED            // Order fulfilment is complete.
}
```

## Why an enum?

- It prevents arbitrary strings such as `"paid-ish"` or `"paymentdone"` from entering the domain.
- `@Enumerated(EnumType.STRING)` in `CommerceOrder` stores the enum name, which is readable and avoids ordinal shifts if enum members are reordered.
- If the enum is stored as an ordinal instead, inserting a new enum constant in the middle can silently change the meaning of existing database values.

## Important gap in the current class

The enum itself does not enforce valid transitions. `CommerceOrder.transitionTo()` currently accepts any non-null or even null status because it simply assigns the value. A state-transition method should eventually enforce an explicit transition graph, but that graph must be agreed with `CommerceService`, cancellation, expiry, payment settlement, and fulfilment flows together. Do not invent transitions in isolation.

**Interview:** Why use `EnumType.STRING` rather than `ORDINAL`? What is a state machine? Why should terminal states such as `FULFILLED` not transition back to `PENDING_PAYMENT`?

---

# 2. CommerceOrder.java

## Purpose

This is the aggregate root for an order. It owns order identity, tenant/customer ownership, monetary totals, shipping address, payment references, timestamps, and its collection of `OrderItem` rows.

```java
package com.finflow.commerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity // Tells JPA/Hibernate that this class is persisted as an entity.
@Table(name = "orders") // Maps it to the orders table.
public class CommerceOrder {

    @Id // Primary key.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Database generates the ID.
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "organisation_id", nullable = false) // Tenant boundary; mandatory in DB.
    private Long organisationId;

    @Column(name = "customer_user_id", nullable = false) // Customer who owns the order.
    private Long customerUserId;

    @Column(name = "order_number", nullable = false, unique = true, length = 40)
    private String orderNumber; // Human-facing identifier, separate from the numeric PK.

    @Enumerated(EnumType.STRING) // Persist enum names, not fragile ordinal positions.
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount; // BigDecimal avoids binary floating-point money errors.

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode; // E.g. INR; currency is part of the monetary meaning.

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 40)
    private String couponCode;

    @Embedded // ShippingAddress fields are stored in this table, not a separate entity table.
    private ShippingAddress shippingAddress;

    @Column(name = "payment_wallet_id")
    private Long paymentWalletId; // External wallet reference; not a JPA relationship here.

    @Column(name = "payment_transaction_id")
    private Long paymentTransactionId; // External transaction reference.

    @Column(name = "created_at", nullable = false)
    private Instant createdAt; // Absolute timestamp, suitable for persistence across time zones.

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    // The OrderItem.order field owns the FK. Cascade persists/removes children with the order.
    // orphanRemoval deletes a child removed from this collection.
    private List<OrderItem> items = new ArrayList<>();

    protected CommerceOrder() {
        // JPA needs a no-argument constructor; protected prevents normal callers using it.
    }

    public CommerceOrder(Long organisationId, Long customerUserId, String currencyCode) {
        // Current source initializes the aggregate. Add input validation before production use.
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
        // Maintain both sides of the bidirectional relationship.
        items.add(item);
        item.setOrder(this);
        // Snapshot price × quantity contributes to the order total.
        totalAmount = totalAmount.add(item.lineTotal());
    }

    public void applyDiscount(String couponCode, BigDecimal discountedTotal) {
        BigDecimal before = this.totalAmount; // Preserve pre-discount amount for calculation.
        this.couponCode = couponCode;
        this.discountAmount = before.subtract(discountedTotal);
        this.totalAmount = discountedTotal;
    }

    public void transitionTo(OrderStatus next) {
        // Current source assigns directly. Production version should validate allowed transitions.
        this.status = next;
    }

    public void recordPayment(Long walletId, Long transactionId) {
        // Save references returned by the wallet/payment integration.
        this.paymentWalletId = walletId;
        this.paymentTransactionId = transactionId;
    }

    public void setShippingAddress(ShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    // Getters expose state to services/mappers without exposing public setters for every field.
    public Long getOrderId() { return orderId; }
    public Long getOrganisationId() { return organisationId; }
    public Long getCustomerUserId() { return customerUserId; }
    public String getOrderNumber() { return orderNumber; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public String getCouponCode() { return couponCode; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public Long getPaymentWalletId() { return paymentWalletId; }
    public Long getPaymentTransactionId() { return paymentTransactionId; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }
}
```

## Fields and annotations

- `@Entity` makes the class managed by JPA. Without it, JPA does not persist it as an entity.
- `@Table` sets the table name explicitly. Without it, a naming strategy chooses a name.
- `@Id` and `@GeneratedValue` define the primary key and its generation strategy.
- `@Column(nullable=false)` adds a schema-level non-null constraint when schema generation/migrations include it. It does not replace request/domain validation.
- `@Enumerated(EnumType.STRING)` stores readable enum names.
- `@Embedded` flattens the value object's fields into the `orders` row.
- `@OneToMany` describes the collection; `mappedBy="order"` says `OrderItem.order` owns the foreign key.
- `cascade=ALL` means persistence operations cascade to items; `orphanRemoval=true` removes child rows removed from the collection.

## Important methods and what if they are missing?

- `addItem`: without setting both sides, the Java object graph and database FK can disagree. Without adding `lineTotal`, the order total stays wrong.
- `applyDiscount`: without checking coupon validity and ensuring discounted total is between zero and original total, a caller could create negative or inflated totals. Coupon validation belongs in the coupon/domain flow too.
- `transitionTo`: unrestricted transitions can produce impossible lifecycle changes. A state machine and concurrency strategy are required.
- `recordPayment`: payment IDs are needed for later refunds/reconciliation. Nullability requirements depend on the actual payment adapter contract.
- `getItems`: returning the mutable internal list lets callers bypass `addItem` and break relationship/total invariants. A later hardening change can return an unmodifiable view, after checking mapper/Hibernate use.

## Current risks to fix deliberately

1. Validate positive organisation/customer IDs, valid 3-letter currency, non-null items, positive quantities, non-negative prices, and discount bounds.
2. Validate order transitions.
3. Consider `@Version` or explicit row locking to protect concurrent order state updates.
4. Random 8-character order suffixes can collide; unique DB constraint prevents duplicate persistence but collision handling/retry is needed.
5. `Instant.now()` is generated in application code; use a consistent clock abstraction if deterministic tests are important.

**Interview:** Why BigDecimal for money? Explain owning vs inverse side of a JPA relationship. What does orphan removal do? Why is an order an aggregate root?

---

# 3. OrderItem.java

## Purpose

An order item records what the customer bought at checkout. It stores product ID plus name and unit-price snapshots so later catalogue edits do not rewrite historical order details.

```java
package com.finflow.commerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity // Persist this class as a row.
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // Many item rows belong to one order. LAZY avoids loading the order whenever an item is loaded.
    @JoinColumn(name = "order_id", nullable = false) // FK column in order_items.
    private CommerceOrder order;

    @Column(name = "product_id", nullable = false)
    private Long productId; // Scalar reference; not a JPA Product relationship in this model.

    @Column(name = "product_name_snapshot", nullable = false, length = 200)
    private String productNameSnapshot; // Historical name at purchase time.

    @Column(name = "unit_price_snapshot", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPriceSnapshot; // Historical unit price at purchase time.

    @Column(nullable = false)
    private int quantity;

    protected OrderItem() {
        // Required by JPA.
    }

    public OrderItem(Long productId, String productNameSnapshot, BigDecimal unitPriceSnapshot, int quantity) {
        // Current source assigns directly. Validate IDs, name, price, and quantity in hardened code.
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.quantity = quantity;
    }

    void setOrder(CommerceOrder order) {
        // Package-private: CommerceOrder.addItem is intended to manage the relationship.
        this.order = order;
    }

    public BigDecimal lineTotal() {
        // Use BigDecimal arithmetic for price × quantity.
        return unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getOrderItemId() { return orderItemId; }
    public Long getProductId() { return productId; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
    public int getQuantity() { return quantity; }
}
```

## Key concepts

- `@ManyToOne` is the owning side because it contains `@JoinColumn` and the FK.
- `FetchType.LAZY` delays loading the parent order until needed. Accessing it after the persistence context closes may cause `LazyInitializationException` if it was not initialized.
- The product is stored as a scalar ID rather than a `@ManyToOne Product`; this avoids coupling historical order lines to a mutable catalogue entity.
- Snapshot fields preserve what was actually purchased.

## What if validation is absent?

A null price makes `lineTotal()` throw `NullPointerException`; zero/negative quantities create invalid order values; blank product names make historical records poor; a null item passed to `CommerceOrder.addItem` causes a failure. Validate at the boundary and in the domain constructor because internal callers can bypass DTO validation.

**Interview:** Why store a product snapshot instead of reading the current Product row? What is the owning side of a bidirectional JPA relationship? LAZY vs EAGER?

---

# 4. Product.java

## Purpose

`Product` represents a sellable catalogue item and its current inventory. The class contains stock mutation methods so services do not directly manipulate the quantity field.

```java
package com.finflow.commerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId; // Tenant ownership.

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity; // Sellable stock remaining.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status; // ACTIVE or INACTIVE.

    @Version // Hibernate includes this version in updates to detect lost updates.
    private Long version;

    protected Product() {
        // JPA constructor.
    }

    public Product(Long organisationId, String name, BigDecimal unitPrice, int availableQuantity, Long categoryId) {
        // Current source does not validate constructor arguments; add domain validation.
        this.organisationId = organisationId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.availableQuantity = availableQuantity;
        this.categoryId = categoryId;
        this.status = ProductStatus.ACTIVE;
    }

    public Long getProductId() { return productId; }
    public Long getOrganisationId() { return organisationId; }
    public Long getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getAvailableQuantity() { return availableQuantity; }
    public ProductStatus getStatus() { return status; }
    public Long getVersion() { return version; }

    public void assignCategory(Long categoryId) {
        // Change the category assignment; service should validate tenant/category ownership.
        this.categoryId = categoryId;
    }

    public void releaseStock(int qty) {
        // Current source increments directly. Validate qty > 0 and guard integer overflow.
        this.availableQuantity += qty;
    }

    public void reserveStock(int qty) {
        // Reject zero/negative requests and prevent stock from becoming negative.
        if (qty <= 0 || availableQuantity < qty) {
            throw new IllegalStateException("Insufficient stock");
        }
        this.availableQuantity -= qty;
    }

    public void updateUnitPrice(BigDecimal unitPrice) {
        // Price must be present and positive in this project's current rule.
        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("Unit price must be positive");
        }
        this.unitPrice = unitPrice;
    }

    public void updateAvailableQuantity(int availableQuantity) {
        // Admin adjustment cannot set negative stock.
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("Available quantity cannot be negative");
        }
        this.availableQuantity = availableQuantity;
    }
}
```

## `@Version` in practical terms

Suppose stock is 1 and two checkout transactions both read version 7 and quantity 1. Both try to reserve one. Hibernate updates with a condition equivalent to `WHERE product_id = ? AND version = 7`. The first update succeeds and advances the version. The second update affects zero rows and Hibernate raises an optimistic-lock exception. The service must handle/retry or return a conflict.

Without `@Version` or a suitable pessimistic lock/atomic SQL update, both transactions can read the same stock and overwrite each other's changes (lost update / overselling risk).

## Current risks

- `releaseStock` accepts negative values and can overflow an `int`.
- `reserveStock` throws `IllegalStateException`; service translates this to an API error. A domain-specific exception would be clearer.
- `@Version` protects updates only when the entity is managed and update uses the version check. It does not replace transaction boundaries or tenant checks.
- If the service performs a stale read then external side effects before commit, optimistic locking cannot undo the external side effect.

**Interview:** Optimistic vs pessimistic locking. What is a lost update? Does `@Version` lock a row for the whole transaction? No: it detects conflicting updates at flush/commit.

---

# 5. ReturnRequest.java

## Purpose

Tracks a customer's return request independently from the order. Its status moves through REQUESTED → APPROVED → COMPLETED, or REQUESTED → REJECTED.

```java
package com.finflow.commerce.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "return_requests")
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    private Long returnId;

    @Column(name = "order_id", nullable = false)
    private Long orderId; // Order being returned; scalar ID in current model.

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId; // Requesting customer.

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReturnRequest() { // JPA constructor.
    }

    public ReturnRequest(Long orderId, Long customerUserId, String reason) {
        // Current source assigns directly; validate positive IDs and nonblank reason.
        this.orderId = orderId;
        this.customerUserId = customerUserId;
        this.reason = reason;
        this.status = ReturnStatus.REQUESTED;
        this.createdAt = Instant.now();
    }

    public void approve() {
        // Only a newly requested return can be approved.
        if (status != ReturnStatus.REQUESTED) {
            throw new IllegalStateException("Only REQUESTED returns can be approved");
        }
        this.status = ReturnStatus.APPROVED;
    }

    public void complete() {
        // Completion is allowed only after approval.
        if (status != ReturnStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED returns can be completed");
        }
        this.status = ReturnStatus.COMPLETED;
    }

    public void reject() {
        // A rejected request cannot later be approved/completed.
        if (status != ReturnStatus.REQUESTED) {
            throw new IllegalStateException("Only REQUESTED returns can be rejected");
        }
        this.status = ReturnStatus.REJECTED;
    }

    public Long getReturnId() { return returnId; }
    public Long getOrderId() { return orderId; }
    public Long getCustomerUserId() { return customerUserId; }
    public String getReason() { return reason; }
    public ReturnStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
```

## Important gap

The status checks protect normal sequential calls but not concurrent approvals. Two transactions can both read REQUESTED and both attempt approval. Add optimistic versioning or a row lock if concurrent administration is possible. The return service must also prevent duplicate refunds/restocks and must verify that the order is eligible for return. Current model is full-order return only; it has no return-line quantities for partial returns.

**Interview:** Why model return as its own entity? What is a state transition invariant? Why are status checks alone insufficient under concurrency?

---

# 6. IdempotencyRecord.java

## Purpose

Stores the association between an idempotency key and the order created by the request. If a client retries the same checkout after a timeout, the service can return the previously created order rather than creating another order/payment attempt.

```java
package com.finflow.commerce.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyRecord {

    @Id // The idempotency key itself is the primary key in the current schema/model.
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId; // Tenant context recorded with the key.

    @Column(name = "order_id", nullable = false)
    private Long orderId; // Previously created order to replay.

    protected IdempotencyRecord() { // JPA constructor.
    }

    public IdempotencyRecord(String idempotencyKey, Long organisationId, Long orderId) {
        // Current source stores the values directly; validate and normalize key before persistence.
        this.idempotencyKey = idempotencyKey;
        this.organisationId = organisationId;
        this.orderId = orderId;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getOrderId() { return orderId; }
}
```

## Current idempotency limitation

The current key is globally unique because it is the primary key. The service currently returns the stored `orderId` when the key exists, but it does not compare the retry's customer, organisation, or request payload with the original request. A different request reusing the same key can incorrectly receive another request's order ID.

A robust design normally stores/validates:

- key scope (often organisation + customer + operation, depending on API contract),
- request fingerprint/hash,
- original response/order reference,
- creation/expiry metadata,
- processing/completed state if requests can be in flight.

Adding `customerUserId` or `requestHash` is a schema change: existing rows need a migration/backfill/default strategy. Do not just add entity fields and assume production DBs will update safely.

**Interview:** What problem does idempotency solve? Why is a unique DB constraint still needed if the application checks first? Why compare a request fingerprint on replay?

---

# 7. IdempotencyRecordRepository.java

## Purpose

Spring Data repository for reading and writing idempotency records.

```java
package com.finflow.commerce.repository;

import com.finflow.commerce.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// JpaRepository<Entity, ID type> supplies save, findById, findAll, delete, etc.
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {

    // Spring Data derives a query from the method name:
    // SELECT ... WHERE idempotency_key = ?
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);
}
```

## Why `Optional`?

The key may not exist. `Optional` makes absence explicit and avoids returning a null entity reference. It does not mean database errors are swallowed.

## Current limitation

This repository only has lookup by key. If the model is changed to scope keys or compare request hashes, add a query method only after deciding the unique-key design and updating all call sites and migrations.

**Interview:** How does Spring Data derive queries? What is the ID generic parameter in `JpaRepository`? Why use Optional?

---

# 8. ProductRepository.java

## Purpose

Provides database access for products, including CRUD, specifications, and a tenant-scoped catalogue listing.

```java
package com.finflow.commerce.repository;

import com.finflow.commerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

// JpaRepository provides CRUD + pagination/sorting methods.
// JpaSpecificationExecutor allows dynamic criteria queries built with Specifications.
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // Derived query: products for one organisation, ordered by product ID ascending.
    List<Product> findByOrganisationIdOrderByProductIdAsc(Long organisationId);
}
```

## What if the specification interface is removed?

Any service using `findAll(Specification)` or dynamic filters will stop compiling. `JpaRepository` alone does not provide the specification executor API.

## Concurrency note

The current repository does not define a pessimistic-lock query. The entity has `@Version`, so optimistic locking can detect competing updates, but a high-contention inventory flow may instead choose a row lock or atomic conditional update. The right choice must be consistent across checkout, cancellation, payment failure, and expiry.

**Interview:** Repository vs DAO? What are JPA Specifications? When would you use pessimistic locking for inventory?

---

# 9. OrderPlacementService.java

## Purpose

Creates a pending-payment order. In one database transaction it checks organisation access, handles a prior idempotency key, loads products, reserves stock, snapshots order items, applies a coupon, saves the order, saves the idempotency record, and creates a payment outbox row. It intentionally does not call the wallet provider in this method.

```java
package com.finflow.commerce.service;

import com.finflow.commerce.dto.OrderItemRequest;
import com.finflow.commerce.dto.ShippingAddressRequest;
import com.finflow.commerce.entity.*;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.payment.PaymentOutcome;
import com.finflow.commerce.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service // Spring creates/manages this service bean.
public class OrderPlacementService {

    private final ProductRepository productRepository;
    private final CommerceOrderRepository orderRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final CouponRepository couponRepository;

    // Constructor injection makes required dependencies explicit and supports unit testing.
    public OrderPlacementService(ProductRepository productRepository,
                                 CommerceOrderRepository orderRepository,
                                 IdempotencyRecordRepository idempotencyRecordRepository,
                                 PaymentOutboxRepository outboxRepository,
                                 CouponRepository couponRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.outboxRepository = outboxRepository;
        this.couponRepository = couponRepository;
    }

    @Transactional // All DB changes below commit together or roll back together.
    public Long placePendingOrder(Long customerUserId,
                                  List<Long> callerOrgIds,
                                  String idempotencyKey,
                                  Long organisationId,
                                  List<OrderItemRequest> items,
                                  ShippingAddressRequest shippingAddress,
                                  String couponCode,
                                  PaymentOutcome sandboxHint) {
        requireOrgAccess(callerOrgIds, organisationId); // Tenant authorization.
        requireIdempotencyKey(idempotencyKey); // Key is mandatory for safe retries.

        var existing = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey.trim());
        if (existing.isPresent()) {
            // Replay path: current code returns old order without validating caller/payload equivalence.
            return existing.get().getOrderId();
        }

        CommerceOrder order = new CommerceOrder(organisationId, customerUserId, "INR");
        if (shippingAddress != null) {
            order.setShippingAddress(toAddress(shippingAddress)); // Convert transport DTO to value object.
        }

        for (OrderItemRequest line : items) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "Product not found: " + line.productId()));
            if (!product.getOrganisationId().equals(organisationId)) {
                throw ApiException.unprocessable("PRODUCT_ORG_MISMATCH", "Product belongs to another organisation");
            }
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw ApiException.unprocessable("PRODUCT_INACTIVE", "Product is not active: " + line.productId());
            }
            try {
                product.reserveStock(line.quantity()); // Mutate managed entity; Hibernate persists at flush/commit.
            } catch (IllegalStateException ex) {
                throw ApiException.conflict("INSUFFICIENT_STOCK", "Not enough stock for product " + line.productId());
            }
            order.addItem(new OrderItem(product.getProductId(), product.getName(), product.getUnitPrice(), line.quantity()));
        }

        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = couponRepository.findByOrganisationIdAndCodeIgnoreCase(organisationId, couponCode.trim())
                    .orElseThrow(() -> ApiException.notFound("COUPON_NOT_FOUND", "Coupon not found: " + couponCode));
            try {
                order.applyDiscount(coupon.getCode(), coupon.apply(order.getTotalAmount()));
            } catch (IllegalStateException ex) {
                throw ApiException.unprocessable("COUPON_INVALID", ex.getMessage());
            }
        }

        order.transitionTo(OrderStatus.PENDING_PAYMENT); // Stock is reserved; payment is next.
        order = orderRepository.save(order);
        idempotencyRecordRepository.save(new IdempotencyRecord(idempotencyKey.trim(), organisationId, order.getOrderId()));
        outboxRepository.save(new PaymentOutbox(order.getOrderId(), sandboxHint == null ? null : sandboxHint.name()));
        return order.getOrderId();
    }

    private static ShippingAddress toAddress(ShippingAddressRequest request) {
        // DTO-to-domain conversion; trim human-entered strings.
        return new ShippingAddress(request.line1().trim(),
                request.line2() == null ? null : request.line2().trim(),
                request.city().trim(),
                request.state() == null ? null : request.state().trim(),
                request.postalCode().trim(),
                request.country());
    }

    private static void requireOrgAccess(List<Long> callerOrgIds, Long organisationId) {
        if (callerOrgIds == null || !callerOrgIds.contains(organisationId)) {
            throw ApiException.forbidden("No access to organisation " + organisationId);
        }
    }

    private static void requireIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw ApiException.badRequest("Idempotency-Key header is required");
        }
    }
}
```

## What `@Transactional` guarantees—and what it does not

It makes the participating database operations atomic when they use the same transaction manager and transaction. If product reservation, order insert, idempotency insert, or outbox insert fails, the DB transaction should roll back.

It does **not** make an external HTTP payment call transactional. That is why the method writes an outbox row and the payment call happens later.

## Critical gaps to fix

1. **Idempotency replay:** currently checks only key; it does not verify customer, tenant, or request hash.
2. **Concurrent duplicate requests:** two requests can both check “not found” before either inserts. The DB primary key prevents duplicate records, but the losing transaction needs deliberate duplicate-key handling and replay.
3. **Inventory locking:** `findById` plus entity `@Version` may detect conflicts at flush, but the service currently does not translate optimistic-lock failures into a clean retry/conflict response.
4. **Lock order:** multiple product rows should be locked/updated in a consistent sorted order if pessimistic locking is adopted, to reduce deadlocks.
5. **Input validation:** null/empty item list, duplicate product lines, invalid quantity, malformed address, and duplicate product IDs need a clear policy.
6. **Coupon concurrency/rules:** ensure coupon validity, expiry, usage limits, and redemption count are handled atomically if applicable.
7. **Currency:** current code hardcodes `INR`; this must be a deliberate business rule, not an accidental constant.

**Interview:** Why should an external payment call not happen inside the DB transaction? Explain atomicity, idempotency, transactional outbox, optimistic locking, and duplicate-key race handling.

---

# 10. CommerceOrderRepository.java

## Purpose

Provides CRUD and three domain-specific query methods for orders.

```java
package com.finflow.commerce.repository;

import com.finflow.commerce.entity.CommerceOrder;
import com.finflow.commerce.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface CommerceOrderRepository extends JpaRepository<CommerceOrder, Long> {

    // Customer's orders within one organisation, newest first.
    List<CommerceOrder> findByOrganisationIdAndCustomerUserIdOrderByCreatedAtDesc(Long organisationId, Long customerUserId);

    // All orders for an organisation, newest first (authorization must be enforced by caller).
    List<CommerceOrder> findByOrganisationIdOrderByCreatedAtDesc(Long organisationId);

    // Candidate orders in selected states created before a cutoff; used by reservation expiry.
    List<CommerceOrder> findByStatusInAndCreatedAtBefore(Collection<OrderStatus> statuses, Instant createdBefore);
}
```

## Derived query explanation

Spring Data parses the method name. `StatusIn` becomes an `IN (...)` predicate; `CreatedAtBefore` becomes `< cutoff`; `OrderByCreatedAtDesc` adds descending sort.

## Current concurrency gap

The repository has no `PESSIMISTIC_WRITE` method. Expiry and payment settlement both read and update order state, so they can race. A candidate query is not a lock. A robust implementation needs to re-fetch/recheck each order under a consistent lock/version strategy inside a transaction before changing status or releasing stock.

**Interview:** Why does a repository interface need no implementation class? What is a derived query? What is the difference between fetching candidate IDs and locking rows for mutation?

---

# 11. PaymentOutbox.java

## Purpose

Stores a durable record that a payment operation must be attempted for an order. The row is created in the same transaction as the order, so the system does not lose the intention to charge if the application crashes immediately after checkout commits.

```java
package com.finflow.commerce.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_outbox")
public class PaymentOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId; // One payment outbox row per order in this model.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status; // PENDING, PROCESSING, DONE, FAILED.

    @Column(nullable = false)
    private int attempts; // Number of processing attempts.

    @Column(name = "last_error", length = 500)
    private String lastError; // Bounded diagnostic detail; avoid secrets/PII.

    @Column(name = "sandbox_hint", length = 20)
    private String sandboxHint; // Test/simulator hint; must not be treated as trusted payment result.

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentOutbox() { // JPA constructor.
    }

    public PaymentOutbox(Long orderId, String sandboxHint) {
        this.orderId = orderId;
        this.status = OutboxStatus.PENDING; // Newly created work has not yet been attempted.
        this.attempts = 0;
        this.sandboxHint = sandboxHint;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markProcessing() {
        this.status = OutboxStatus.PROCESSING;
        this.attempts++;
        this.updatedAt = Instant.now();
    }

    public void markDone() {
        this.status = OutboxStatus.DONE;
        this.lastError = null;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = OutboxStatus.FAILED;
        this.lastError = truncate(error);
        this.updatedAt = Instant.now();
    }

    public void markPending(String error) {
        this.status = OutboxStatus.PENDING;
        this.lastError = truncate(error);
        this.updatedAt = Instant.now();
    }

    private static String truncate(String error) {
        // Protect the DB column limit and prevent oversized exception messages.
        if (error == null) return null;
        return error.length() <= 500 ? error : error.substring(0, 500);
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public OutboxStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public String getSandboxHint() { return sandboxHint; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

## State meanings

- `PENDING`: work is waiting to be attempted/retried.
- `PROCESSING`: a worker has claimed the work.
- `DONE`: operation completed successfully.
- `FAILED`: operation was definitively failed or administratively stopped; whether it is retryable must be explicit.

## Critical issue in the current payment service

The current `PaymentSettlementService` invokes the external payment provider and only afterwards calls `outbox.markProcessing()` inside the DB transaction. That means the outbox is not actually claimed before the external call. Two concurrent requests can both call the provider. `markProcessing` must be part of an atomic claim/lock before the external call, and the provider request must use a stable idempotency key.

The outbox also has no `@Version` or atomic claim query in this source. A status field alone does not prevent two workers from reading PENDING concurrently.

**Interview:** Explain the transactional outbox pattern. Why does it not guarantee exactly-once delivery? How do at-least-once delivery and provider idempotency work together?

---

# 12. PaymentSettlementService.java

## Purpose

Coordinates payment charging/refunding with order state updates. It reads an order, calls `PaymentPort`, maps the provider outcome to order/outbox state, and publishes an order-paid event when payment is approved.

```java
package com.finflow.commerce.service;

import com.finflow.commerce.entity.*;
import com.finflow.commerce.event.*;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.payment.*;
import com.finflow.commerce.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PaymentSettlementService {

    private final CommerceOrderRepository orderRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final ProductRepository productRepository;
    private final PaymentPort paymentPort; // Port hides which payment provider/adapter is used.
    private final TransactionTemplate transactionTemplate; // Explicit short DB transactions.
    private final CommerceEventPublisher eventPublisher;

    public PaymentSettlementService(CommerceOrderRepository orderRepository,
                                    PaymentOutboxRepository outboxRepository,
                                    ProductRepository productRepository,
                                    PaymentPort paymentPort,
                                    TransactionTemplate transactionTemplate,
                                    CommerceEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.productRepository = productRepository;
        this.paymentPort = paymentPort;
        this.transactionTemplate = transactionTemplate;
        this.eventPublisher = eventPublisher;
    }

    public CommerceOrder settleCharge(Long orderId, String accessToken) {
        // Read order snapshot. This is currently outside a transaction/lock and can become stale.
        CommerceOrder snapshot = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found: " + orderId));

        if (snapshot.getStatus() != OrderStatus.PENDING_PAYMENT && snapshot.getStatus() != OrderStatus.ACTION_REQUIRED) {
            // Return current state for already-processed/non-payable orders.
            return transactionTemplate.execute(status -> {
                CommerceOrder managed = orderRepository.findById(orderId).orElseThrow();
                managed.getItems().size(); // Initialize lazy collection before transaction closes.
                return managed;
            });
        }

        PaymentOutbox outbox = outboxRepository.findByOrderId(orderId)
                .orElseThrow(() -> ApiException.conflict("OUTBOX_MISSING", "Payment outbox missing for order " + orderId));

        // External HTTP call. It is correctly outside a long DB transaction,
        // but the outbox has NOT been claimed yet in the current implementation.
        PaymentResult payment = paymentPort.charge(new PaymentChargeRequest(
                snapshot.getCustomerUserId(), snapshot.getTotalAmount(), snapshot.getCurrencyCode(),
                "commerce-pay-" + snapshot.getOrderId(), accessToken, parseHint(outbox.getSandboxHint())
        ));

        // Apply DB state changes in a short transaction after the external call.
        CommerceOrder settled = transactionTemplate.execute(status -> applyChargeResult(orderId, payment));

        // Event is published after the DB transaction, avoiding publication before commit.
        // But a process crash between commit and publish can lose this event; use an event outbox for durability.
        if (settled != null && settled.getStatus() == OrderStatus.CONFIRMED) {
            eventPublisher.publish(CommerceEvents.of(settled, CommerceEventType.ORDER_PAID));
        }
        return settled;
    }

    public void settleRefund(CommerceOrder order, String accessToken) {
        // Stable refund key helps the payment provider deduplicate retries.
        PaymentResult refund = paymentPort.refund(new PaymentRefundRequest(
                order.getCustomerUserId(), order.getPaymentWalletId(), order.getTotalAmount(),
                "commerce-refund-" + order.getOrderId(), accessToken
        ));
        if (refund.outcome() != PaymentOutcome.APPROVED) {
            throw ApiException.conflict("REFUND_FAILED",
                    "Wallet refund failed: " + (refund.failureCode() == null ? refund.outcome() : refund.failureCode()));
        }
    }

    private CommerceOrder applyChargeResult(Long orderId, PaymentResult payment) {
        CommerceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND", "Order not found: " + orderId));
        PaymentOutbox outbox = outboxRepository.findByOrderId(orderId)
                .orElseThrow(() -> ApiException.conflict("OUTBOX_MISSING", "Payment outbox missing for order " + orderId));

        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PAYMENT_FAILED) {
            return order; // Idempotent-looking guard, but it does not compensate an already-approved duplicate charge.
        }

        outbox.markProcessing(); // Current code claims too late: after provider call.
        switch (payment.outcome()) {
            case APPROVED -> {
                order.recordPayment(payment.walletId(), payment.walletTransactionId());
                order.transitionTo(OrderStatus.CONFIRMED);
                outbox.markDone();
            }
            case DECLINED -> {
                releaseStock(order); // Definitive decline releases reserved stock.
                order.transitionTo(OrderStatus.PAYMENT_FAILED);
                outbox.markFailed(payment.failureCode() == null ? "DECLINED" : payment.failureCode());
            }
            case UNCERTAIN -> {
                // Do not release stock: provider may have charged; reconcile before deciding.
                order.transitionTo(OrderStatus.ACTION_REQUIRED);
                outbox.markPending(payment.failureCode() == null ? "UNCERTAIN" : payment.failureCode());
            }
        }
        CommerceOrder saved = orderRepository.save(order);
        saved.getItems().size(); // Ensure collection can be mapped after transaction ends.
        return saved;
    }

    private void releaseStock(CommerceOrder order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "Product missing during stock release"));
            product.releaseStock(item.getQuantity());
        }
    }

    private static PaymentOutcome parseHint(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return PaymentOutcome.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null; // Invalid sandbox hint is ignored; consider rejecting invalid persisted values instead.
        }
    }
}
```

## Critical race and failure scenarios

### Race 1: duplicate payment calls

Two requests read PENDING_PAYMENT, both call `paymentPort.charge`, then both try to settle. The stable provider idempotency key may protect against duplicate charges only if the provider truly enforces it. The application should also atomically claim the outbox before calling the provider.

### Race 2: expiry vs payment

Expiry may release stock and mark the order EXPIRED while payment is in flight. Later the payment call can return APPROVED. The current `applyChargeResult` does not reject EXPIRED status before attempting `transitionTo(CONFIRMED)`; because the current transition method is unrestricted, it can confirm an expired order after stock was released. This is one of the most important correctness defects.

### Race 3: approved payment but DB failure

The provider may approve the charge, then the DB transaction can fail. A retry must use the same provider idempotency key and reconciliation must be able to discover the original provider transaction. A compensation/refund path may be needed if the order can no longer be fulfilled.

### Event durability

Publishing after commit avoids announcing an uncommitted order, but a crash after DB commit and before event publication loses the event. A separate durable event outbox is the usual solution.

### Refund idempotency

Refunds need their own durable state/idempotency record. A retry after timeout must not refund twice. `settleRefund` currently calls the provider but does not persist a refund result/state itself.

**Interview:** Why not hold a DB transaction open during an HTTP call? Explain uncertain payment outcome, provider idempotency, compensation, reconciliation, outbox, and saga-style workflows.

---

# 13. ReservationExpiryService.java

## Purpose

Finds orders whose payment reservation window has elapsed, releases the reserved product quantities, moves the order to EXPIRED, updates the payment outbox, and publishes an expiry event.

```java
package com.finflow.commerce.service;

import com.finflow.commerce.entity.*;
import com.finflow.commerce.event.*;
import com.finflow.commerce.exception.ApiException;
import com.finflow.commerce.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

@Service
public class ReservationExpiryService {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryService.class);

    // Only these states currently represent an order holding stock while awaiting payment.
    private static final EnumSet<OrderStatus> RESERVING = EnumSet.of(
            OrderStatus.PENDING_PAYMENT, OrderStatus.ACTION_REQUIRED);

    private final CommerceOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final CommerceEventPublisher eventPublisher;

    public ReservationExpiryService(CommerceOrderRepository orderRepository,
                                     ProductRepository productRepository,
                                     PaymentOutboxRepository outboxRepository,
                                     CommerceEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Returns how many candidate orders were processed as expired. */
    @Transactional // Stock, order state, and outbox update should commit/rollback together.
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
        // Restore every line's reserved quantity.
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND", "Product missing during expiry"));
            product.releaseStock(item.getQuantity());
        }

        order.transitionTo(OrderStatus.EXPIRED); // Current transition method does not validate.
        orderRepository.save(order);

        // Stop pending payment work unless payment is already marked DONE.
        outboxRepository.findByOrderId(order.getOrderId()).ifPresent(outbox -> {
            if (outbox.getStatus() != OutboxStatus.DONE) {
                outbox.markFailed("RESERVATION_TTL_EXPIRED");
                outboxRepository.save(outbox);
            }
        });

        // Current event publish occurs inside the DB transaction because this method is @Transactional.
        // A consumer may observe an event for a transaction that later rolls back.
        eventPublisher.publish(CommerceEvents.of(order, CommerceEventType.ORDER_EXPIRED));
    }
}
```

## Critical gaps

1. The candidate query does not lock orders. Payment settlement and expiry can race.
2. Each candidate must be reloaded/rechecked under the same lock/version protocol used by payment settlement.
3. Expiry must not expire an order whose payment request is currently PROCESSING or whose provider outcome is uncertain until reconciliation policy permits it.
4. Product stock restoration must be exactly once. If the job runs twice or two scheduler instances process the same order, stock can be incremented twice unless the order state transition and stock update are atomically protected.
5. The event is published inside the transaction. If DB rollback follows, consumers may receive a false expiry event. Publish after commit or, preferably, persist a durable event outbox in the same transaction.
6. Large result sets need batching/paging/claiming; processing every stale order in one transaction can hold locks too long.

**Interview:** Why do scheduled jobs need distributed locking? What is idempotent job processing? Why must stock release and order expiry happen atomically?

---

# End-to-end flow: how these classes work together

1. `OrderPlacementService` receives checkout details.
2. `ProductRepository` loads products; `Product.reserveStock()` reduces available quantity.
3. `OrderItem` stores product/name/price snapshots; `CommerceOrder.addItem()` maintains the relationship and total.
4. `CommerceOrder` moves to `PENDING_PAYMENT`.
5. The order, `IdempotencyRecord`, and `PaymentOutbox` are persisted in the same transaction.
6. `PaymentSettlementService` calls the payment port and maps APPROVED / DECLINED / UNCERTAIN to order/outbox outcomes.
7. If payment is approved, the order becomes `CONFIRMED`; if declined, stock is released and the order becomes `PAYMENT_FAILED`; if uncertain, the order remains in an action-required state for reconciliation.
8. `ReservationExpiryService` eventually releases stock for genuinely unpaid stale reservations.
9. `ReturnRequest` models the separate post-purchase return workflow.

## Priority corrections before calling this production-ready

1. Define and enforce legal order transitions; specifically prevent EXPIRED/CANCELLED orders from becoming CONFIRMED without an explicit compensation/recovery workflow.
2. Atomically claim the payment outbox **before** calling the provider.
3. Use the same stable provider idempotency key for retries and reconcile uncertain outcomes.
4. Make payment settlement and reservation expiry share a consistent order/product locking strategy.
5. Make stock release exactly-once through atomic state transitions and transaction protection.
6. Scope idempotency to the correct caller/tenant/operation and compare request fingerprints.
7. Make event delivery durable with an event outbox; after-commit publication alone can lose events.
8. Add DB migrations for any new columns, indexes, constraints, or version fields; do not rely on `ddl-auto=update` in production.
9. Add concurrency tests for duplicate checkout, duplicate payment invocation, payment-vs-expiry race, duplicate expiry, duplicate refund, and concurrent return approval.

## Test checklist after implementation changes

- [ ] All existing Maven tests pass.
- [ ] Invalid product quantity does not change stock.
- [ ] Two concurrent checkouts for the final unit cannot both succeed.
- [ ] Same idempotency key + same request returns the same order.
- [ ] Same key + different customer/tenant/payload is rejected according to the API contract.
- [ ] Duplicate payment requests do not double-charge.
- [ ] Payment timeout/UNCERTAIN does not prematurely release stock.
- [ ] Expiry racing with payment cannot confirm an expired order or release stock twice.
- [ ] Refund retry cannot refund twice.
- [ ] Return approval/completion cannot cause duplicate refund/restock.
- [ ] Event consumers do not see events for rolled-back transactions.

---

# Important annotation quick reference

| Annotation / feature | Purpose | If absent / misused |
|---|---|---|
| `@Entity` | Makes class a JPA entity | JPA will not map it as an entity |
| `@Table` | Explicit table mapping | Naming strategy chooses table name |
| `@Id` | Primary key | Entity mapping is invalid |
| `@GeneratedValue` | Generates PK | Caller/database must otherwise assign it |
| `@Column` | Column mapping and constraints | Defaults apply; DB constraints may not match intent |
| `@Enumerated(STRING)` | Stores enum name | Default is ordinal; enum reordering can corrupt meaning |
| `@Embedded` | Flattens value object into owner row | Separate entity/table relationship would be needed |
| `@OneToMany` | Parent collection mapping | Child collection is not persisted as this relationship |
| `@ManyToOne` | Child-to-parent mapping | FK relationship mapping is absent |
| `@JoinColumn` | Defines FK column | Provider naming defaults are used |
| `@Version` | Detects concurrent updates | Lost updates can go undetected unless another lock strategy exists |
| `@Service` | Registers service bean | Component scanning will not discover it as a service bean |
| `@Transactional` | Defines DB transaction boundary | Operations may commit separately; rollback/atomicity expectations fail |

