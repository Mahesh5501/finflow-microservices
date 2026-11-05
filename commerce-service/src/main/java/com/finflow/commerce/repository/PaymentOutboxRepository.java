package com.finflow.commerce.repository;

import com.finflow.commerce.entity.OutboxStatus;
import com.finflow.commerce.entity.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {

    Optional<PaymentOutbox> findByOrderId(Long orderId);

    List<PaymentOutbox> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
