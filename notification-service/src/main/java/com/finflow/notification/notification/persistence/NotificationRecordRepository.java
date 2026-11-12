package com.finflow.notification.notification.persistence;

import com.finflow.notification.notification.domain.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {

    boolean existsByEventTypeAndOrderId(String eventType, Long orderId);

    List<NotificationRecord> findByCustomerUserIdOrderByCreatedAtDesc(Long customerUserId);
}
