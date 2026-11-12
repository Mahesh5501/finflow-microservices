
package com.finflow.commerce.integration.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka implementation of CommerceEventPublisher.
 * It publishes CommerceDomainEvent objects to the configured Kafka topic.
 * This implementation is active only when `commerce.events.kafka-enabled=true`.
 */
@Component
@ConditionalOnProperty(
        name = "commerce.events.kafka-enabled",
        havingValue = "true"
)
public class KafkaCommerceEventPublisher implements CommerceEventPublisher {

    private final KafkaTemplate<String, CommerceDomainEvent> kafkaTemplate; // Spring Kafka abstraction used to send messages to Kafka.

    private final String topic; // Kafka topic to which Commerce events are published. Default value is "commerce.events".

    // Constructor injection. Spring provides the KafkaTemplate and reads the topic name from application configuration.
    public KafkaCommerceEventPublisher(KafkaTemplate<String, CommerceDomainEvent> kafkaTemplate, @Value("${commerce.events.topic:commerce.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate; // Spring Kafka abstraction used to send messages to Kafka.
        this.topic = topic; // Kafka topic to which Commerce events are published.
    }

    // Publishes one Commerce domain event to Kafka. The event is published to the configured Kafka topic. The event is published with the orderId as the Kafka message key. Kafka uses the key when choosing a partition. Messages with the same key are routed to the same partition, which helps preserve per-order ordering within that partition. send() is asynchronous. This method currently does not wait for the broker acknowledgement or inspect the send result.
    @Override
    public void publish(CommerceDomainEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.orderId()), event);
    }
}
