package com.finflow.commerce.repository;

import com.finflow.commerce.entity.CommerceOrder;
import com.finflow.commerce.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface CommerceOrderRepository extends JpaRepository<CommerceOrder, Long> {

    List<CommerceOrder> findByOrganisationIdAndCustomerUserIdOrderByCreatedAtDesc(
            Long organisationId,
            Long customerUserId
    );

    List<CommerceOrder> findByOrganisationIdOrderByCreatedAtDesc(Long organisationId);

    List<CommerceOrder> findByStatusInAndCreatedAtBefore(Collection<OrderStatus> statuses, Instant createdBefore);
}
