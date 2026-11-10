
package com.finflow.commerce.entity.order;

import com.finflow.commerce.exception.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
