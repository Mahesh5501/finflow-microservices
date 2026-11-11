package com.finflow.commerce.returns.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity representing a customer's return request.
 *
 * Maps to the return_requests database table.
 *
 * Responsibilities:
 * 1. Store return-request information.
 * 2. Track the return lifecycle status.
 * 3. Enforce valid status changes through domain methods.
 *
 * Returns domain state owned by ReturnService and stored through
 * ReturnRequestRepository. Scalar order/customer IDs avoid a cross-feature JPA graph;
 * they do not create foreign keys. Domain methods permit only REQUESTED approval or
 * rejection and APPROVED completion; ReturnService maps invalid transitions to conflicts.
 * @Entity/@Table retain return_requests and @Id/IDENTITY select the generated key.
 * Without these mappings ORM discovery/identity fails; a separate persistence model
 * would add duplication without a second storage need. No optimistic version or
 * single-return-per-order constraint exists in this model.
 */
@Entity
@Table(name = "return_requests")
public class ReturnRequest {

    /**
     * Primary key of the return request.
     *
     * @Id marks this field as the primary key.
     *
     * @GeneratedValue:
     * The database generates the ID when a new record is inserted.
     *
     * IDENTITY:
     * Uses the database's identity/auto-increment mechanism.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    private Long returnId;

    /**
     * ID of the order for which the return was requested.
     *
     * This is stored as a scalar ID rather than a JPA relationship
     * to CommerceOrder in the current design.
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * ID of the customer who submitted the return request.
     */
    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    /**
     * Customer's explanation for requesting a return.
     *
     * Database column is mandatory and limited to 500 characters.
     */
    @Column(nullable = false, length = 500)
    private String reason;

    /**
     * Current lifecycle status of the return request.
     *
     * EnumType.STRING stores values such as:
     * REQUESTED, APPROVED, REJECTED, COMPLETED.
     *
     * STRING is safer than ORDINAL because changing enum order
     * does not silently change the meaning of stored values.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnStatus status;

    /**
     * Time when the return request was created.
     *
     * Instant represents a point in time using UTC.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Protected no-argument constructor required by JPA.
     *
     * JPA uses it when loading an entity from the database.
     *
     * Protected prevents ordinary application code from
     * directly creating an empty ReturnRequest.
     */
    protected ReturnRequest() {
    }

    /**
     * Creates a new return request.
     *
     * New return requests always start in REQUESTED status.
     */
    public ReturnRequest(
            Long orderId,
            Long customerUserId,
            String reason
    ) {

        this.orderId = orderId;
        this.customerUserId = customerUserId;
        this.reason = reason;

        // Every new return starts in the REQUESTED state.
        this.status = ReturnStatus.REQUESTED;

        // Record creation time.
        this.createdAt = Instant.now();
    }

    /**
     * Approves a return request.
     *
     * Only REQUESTED returns can be approved.
     *
     * Invalid transition:
     * APPROVED -> APPROVED
     * REJECTED -> APPROVED
     * COMPLETED -> APPROVED
     */
    public void approve() {

        if (status != ReturnStatus.REQUESTED) {
            throw new IllegalStateException(
                    "Only REQUESTED returns can be approved"
            );
        }

        this.status = ReturnStatus.APPROVED;
    }

    /**
     * Completes an approved return.
     *
     * This method is called after the refund flow succeeds
     * and the return completion process is finalized.
     *
     * Only APPROVED returns can be completed.
     */
    public void complete() {

        if (status != ReturnStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only APPROVED returns can be completed"
            );
        }

        this.status = ReturnStatus.COMPLETED;
    }

    /**
     * Rejects a return request.
     *
     * Only REQUESTED returns can be rejected.
     */
    public void reject() {

        if (status != ReturnStatus.REQUESTED) {
            throw new IllegalStateException(
                    "Only REQUESTED returns can be rejected"
            );
        }

        this.status = ReturnStatus.REJECTED;
    }

    /*
     * Getters
     *
     * These expose entity values without allowing callers
     * to directly change the entity's fields.
     */

    public Long getReturnId() {
        return returnId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getCustomerUserId() {
        return customerUserId;
    }

    public String getReason() {
        return reason;
    }

    public ReturnStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
