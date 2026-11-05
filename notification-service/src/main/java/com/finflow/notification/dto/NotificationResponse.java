package com.finflow.notification.dto;

import com.finflow.notification.entity.NotificationRecord;
import com.finflow.notification.entity.NotificationStatus;

import java.time.Instant;

public record NotificationResponse(
        Long notificationId,
        Long customerUserId,
        Long orderId,
        String orderNumber,
        String eventType,
        String subject,
        String body,
        NotificationStatus status,
        Instant createdAt
) {
    public static NotificationResponse from(NotificationRecord record) {
        return new NotificationResponse(
                record.getNotificationId(),
                record.getCustomerUserId(),
                record.getOrderId(),
                record.getOrderNumber(),
                record.getEventType(),
                record.getSubject(),
                record.getBody(),
                record.getStatus(),
                record.getCreatedAt()
        );
    }
}
