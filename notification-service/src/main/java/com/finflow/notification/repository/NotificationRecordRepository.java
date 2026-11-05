package com.finflow.notification.repository;

import com.finflow.notification.entity.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {

    boolean existsByEventTypeAndOrderId(String eventType, Long orderId);

    List<NotificationRecord> findByCustomerUserIdOrderByCreatedAtDesc(Long customerUserId);
}
