package com.finflow.notification.service;

import com.finflow.notification.dto.NotificationResponse;
import com.finflow.notification.event.CommerceDomainEvent;
import com.finflow.notification.event.CommerceEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class NotificationDispatcherTest {

    @Autowired
    private NotificationDispatcher dispatcher;

    @Test
    void handle_persistsAndIsIdempotent() {
        CommerceDomainEvent event = new CommerceDomainEvent(
                CommerceEventType.ORDER_PAID,
                501L,
                "ORD-TEST01",
                10L,
                99L,
                new BigDecimal("120.00"),
                "INR",
                Instant.now()
        );

        NotificationResponse first = dispatcher.handle(event);
        NotificationResponse second = dispatcher.handle(event);

        assertEquals("ORDER_PAID", first.eventType());
        assertTrue(first.subject().contains("ORD-TEST01"));
        assertNull(second);

        List<NotificationResponse> inbox = dispatcher.listForUser(99L);
        assertEquals(1, inbox.stream().filter(n -> n.orderId().equals(501L)).count());
    }

    @Test
    void handle_fulfilledAndReturn_createDistinctRows() {
        dispatcher.handle(new CommerceDomainEvent(
                CommerceEventType.ORDER_FULFILLED, 502L, "ORD-F", 10L, 7L,
                new BigDecimal("50.00"), "INR", Instant.now()
        ));
        dispatcher.handle(new CommerceDomainEvent(
                CommerceEventType.RETURN_COMPLETED, 502L, "ORD-F", 10L, 7L,
                new BigDecimal("50.00"), "INR", Instant.now()
        ));

        List<NotificationResponse> inbox = dispatcher.listForUser(7L);
        assertTrue(inbox.stream().anyMatch(n -> "ORDER_FULFILLED".equals(n.eventType())));
        assertTrue(inbox.stream().anyMatch(n -> "RETURN_COMPLETED".equals(n.eventType())));
    }
}
