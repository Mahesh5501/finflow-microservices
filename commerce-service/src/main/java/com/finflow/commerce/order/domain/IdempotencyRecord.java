package com.finflow.commerce.order.domain;

import com.finflow.commerce.exception.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Order domain persistence record created by OrderPlacementService in the same
 * transaction as stock/order/payment work. The supplied key is global; owner, seller
 * and payload hash are compared before replay. Depends only on JPA and ApiException.
 * @Entity/@Table retain idempotency_keys; @Id uses the caller key directly, so there
 * is intentionally no @GeneratedValue. Removing the primary-key constraint would
 * allow duplicate key rows. A composite scoped key is an alternative requiring an
 * explicit schema/contract migration. This record prevents sequential duplicate
 * creation, but is not itself an atomic remote-payment or concurrency guarantee.
 */
@Entity
@Table(name = "idempotency_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    public IdempotencyRecord(
            String idempotencyKey,
            Long organisationId,
            Long customerUserId,
            Long orderId,
            String requestHash
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.trim().length() > 100) {
            throw ApiException.badRequest("Idempotency key must contain 1 to 100 characters");
        }

        if (organisationId == null || organisationId <= 0) {
            throw ApiException.badRequest("Organisation ID must be positive");
        }

        if (customerUserId == null || customerUserId <= 0) {
            throw ApiException.badRequest("Customer user ID must be positive");
        }

        if (orderId == null || orderId <= 0) {
            throw ApiException.badRequest("Order ID must be positive");
        }

        if (requestHash == null || !requestHash.matches("[a-fA-F0-9]{64}")) {
            throw ApiException.badRequest("A valid SHA-256 request hash is required");
        }

        this.idempotencyKey = idempotencyKey.trim();
        this.organisationId = organisationId;
        this.customerUserId = customerUserId;
        this.orderId = orderId;
        this.requestHash = requestHash.toLowerCase();
    }
}
