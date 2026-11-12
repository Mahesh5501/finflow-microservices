package com.finflow.notification.integration.kafka;

import com.finflow.notification.integration.event.CommerceDomainEvent;
import com.finflow.notification.notification.application.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.kafka-enabled", havingValue = "true")
public class CommerceEventListener {

    private static final Logger log = LoggerFactory.getLogger(CommerceEventListener.class);

    private final NotificationDispatcher dispatcher;

    public CommerceEventListener(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @KafkaListener(
            topics = "${notification.events.topic}",
            groupId = "${notification.events.group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onCommerceEvent(CommerceDomainEvent event) {
        log.info("Received commerce event type={} orderId={}", event.type(), event.orderId());
        dispatcher.handle(event);
    }
}
