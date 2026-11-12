package com.finflow.notification.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_event_order",
                columnNames = {"event_type", "order_id"}
        )
)
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_number", length = 40)
    private String orderNumber;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 1000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationRecord() {
    }

    public NotificationRecord(
            Long customerUserId,
            Long orderId,
            String orderNumber,
            String eventType,
            String subject,
            String body
    ) {
        this.customerUserId = customerUserId;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.eventType = eventType;
        this.subject = subject;
        this.body = body;
        this.status = NotificationStatus.SENT;
        this.createdAt = Instant.now();
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public Long getCustomerUserId() {
        return customerUserId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
