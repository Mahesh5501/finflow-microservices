
package com.finflow.commerce.integration.event;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer configuration for Commerce domain events.
 * This configuration is loaded only when Kafka publishing is explicitly enabled.
 */
@Configuration
@ConditionalOnProperty(
        name = "commerce.events.kafka-enabled",
        havingValue = "true"
)
public class KafkaEventConfig {

    /**
     * Creates KafkaAdmin.
     * KafkaAdmin connects to the Kafka cluster and manages Kafka administrative operations, such as topic creation.
     */
    @Bean
    KafkaAdmin kafkaAdmin(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) { // Address of the Kafka cluster.
        return new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)); // KafkaAdmin requires the Kafka bootstrap-server address.
    }

    @Bean
    ProducerFactory<String, CommerceDomainEvent> commerceEventProducerFactory(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) { // Address of the Kafka cluster.
        Map<String, Object> props = new HashMap<>(); // Kafka producer properties.
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); // Address of the Kafka cluster.
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class); // Serialize message keys as strings.
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class); // Serialize CommerceDomainEvent values as JSON.
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false); // Do not add Spring type-information headers to JSON messages.
        return new DefaultKafkaProducerFactory<>(props); // Create the Kafka producer factory with these properties.
    }
    /**
     * Creates the KafkaTemplate used by KafkaCommerceEventPublisher.
     * The template uses the producer factory defined above.
     */
    @Bean
    KafkaTemplate<String, CommerceDomainEvent>
    commerceEventKafkaTemplate(
            ProducerFactory<String, CommerceDomainEvent> commerceEventProducerFactory // Producer factory for Commerce domain events.
    ) {
        return new KafkaTemplate<>(commerceEventProducerFactory); // Create the Kafka template with the producer factory.
    }

    /**
     * Declares the Commerce events topic.
     * KafkaAdmin can create this topic when it connects to the Kafka cluster and topic creation is permitted.
     */
    @Bean
    NewTopic commerceEventsTopic(@Value("${commerce.events.topic:commerce.events}") String topic) { // Configurable topic name.
        return TopicBuilder.name(topic).partitions(3).replicas(1).build(); // Topic configuration: Configurable topic name, three partitions, one replica.
    }
}
