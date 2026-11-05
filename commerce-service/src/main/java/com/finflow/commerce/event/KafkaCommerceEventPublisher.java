package com.finflow.commerce.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "commerce.events.kafka-enabled", havingValue = "true")
public class KafkaCommerceEventPublisher implements CommerceEventPublisher {

    private final KafkaTemplate<String, CommerceDomainEvent> kafkaTemplate;
    private final String topic;

    public KafkaCommerceEventPublisher(
            KafkaTemplate<String, CommerceDomainEvent> kafkaTemplate,
            @Value("${commerce.events.topic:commerce.events}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(CommerceDomainEvent event) {
        // Key by orderId so partitions keep per-order ordering.
        kafkaTemplate.send(topic, String.valueOf(event.orderId()), event);
    }
}
