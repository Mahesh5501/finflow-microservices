package com.finflow.notification.notification.application;

import com.finflow.notification.notification.dto.NotificationResponse;
import com.finflow.notification.notification.domain.NotificationRecord;
import com.finflow.notification.integration.event.CommerceDomainEvent;
import com.finflow.notification.integration.mail.MailChannel;
import com.finflow.notification.notification.persistence.NotificationRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Turns commerce domain events into persisted + "sent" notifications.
 * Idempotent on (eventType, orderId) so Kafka redelivery is safe.
 */
@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationRecordRepository repository;
    private final MailChannel mailChannel;

    public NotificationDispatcher(NotificationRecordRepository repository, MailChannel mailChannel) {
        this.repository = repository;
        this.mailChannel = mailChannel;
    }

    @Transactional
    public NotificationResponse handle(CommerceDomainEvent event) {
        String eventType = event.type().name();
        if (repository.existsByEventTypeAndOrderId(eventType, event.orderId())) {
            log.info("Skip duplicate notification eventType={} orderId={}", eventType, event.orderId());
            return null;
        }

        String subject = subjectFor(event);
        String body = bodyFor(event);
        NotificationRecord record = new NotificationRecord(
                event.customerUserId(),
                event.orderId(),
                event.orderNumber(),
                eventType,
                subject,
                body
        );
        try {
            record = repository.saveAndFlush(record);
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent duplicate notification eventType={} orderId={}", eventType, event.orderId());
            return null;
        }
        mailChannel.send(event.customerUserId(), subject, body);
        return NotificationResponse.from(record);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(Long customerUserId) {
        return repository.findByCustomerUserIdOrderByCreatedAtDesc(customerUserId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    private static String subjectFor(CommerceDomainEvent event) {
        return switch (event.type()) {
            case ORDER_PAID -> "Order paid: " + event.orderNumber();
            case ORDER_CANCELLED -> "Order cancelled: " + event.orderNumber();
            case ORDER_EXPIRED -> "Order expired: " + event.orderNumber();
            case ORDER_FULFILLED -> "Order shipped: " + event.orderNumber();
            case RETURN_COMPLETED -> "Return completed: " + event.orderNumber();
        };
    }

    private static String bodyFor(CommerceDomainEvent event) {
        return switch (event.type()) {
            case ORDER_PAID -> "We received payment of "
                    + event.totalAmount() + " " + event.currencyCode()
                    + " for order " + event.orderNumber() + ".";
            case ORDER_CANCELLED -> "Your order " + event.orderNumber() + " was cancelled. Any debit will be refunded.";
            case ORDER_EXPIRED -> "Payment window closed for order " + event.orderNumber()
                    + ". Reserved stock was released — place a new order to try again.";
            case ORDER_FULFILLED -> "Good news — order " + event.orderNumber() + " is on its way.";
            case RETURN_COMPLETED -> "Your return for order " + event.orderNumber()
                    + " is complete. Refund of " + event.totalAmount() + " " + event.currencyCode() + " is processed.";
        };
    }
}
