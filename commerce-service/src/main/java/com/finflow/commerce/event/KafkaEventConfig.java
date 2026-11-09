
package com.finflow.commerce.event;

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
 *
 * This configuration is loaded only when Kafka publishing
 * is explicitly enabled.
 */
@Configuration
@ConditionalOnProperty(
        name = "commerce.events.kafka-enabled",
        havingValue = "true"
)
public class KafkaEventConfig {

    /**
     * Creates KafkaAdmin.
     *
     * KafkaAdmin connects to the Kafka cluster and manages
     * Kafka administrative operations, such as topic creation.
     */
    @Bean
    KafkaAdmin kafkaAdmin(
            @Value("${spring.kafka.bootstrap-servers}")
            String bootstrapServers
    ) {

        // KafkaAdmin requires the Kafka bootstrap-server address.
        return new KafkaAdmin(
                Map.of(
                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                        bootstrapServers
                )
        );
    }

    /**
     * Creates the producer factory for Commerce domain events.
     *
     * It defines how message keys and values are serialized.
     */
    @Bean
    ProducerFactory<String, CommerceDomainEvent>
    commerceEventProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}")
            String bootstrapServers
    ) {

        // Kafka producer properties.
        Map<String, Object> props = new HashMap<>();

        // Address of the Kafka cluster.
        props.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        // Serialize message keys as strings.
        props.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        // Serialize CommerceDomainEvent values as JSON.
        props.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class
        );

        // Do not add Spring type-information headers to JSON messages.
        props.put(
                JsonSerializer.ADD_TYPE_INFO_HEADERS,
                false
        );

        // Create the Kafka producer factory with these properties.
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Creates the KafkaTemplate used by KafkaCommerceEventPublisher.
     *
     * The template uses the producer factory defined above.
     */
    @Bean
    KafkaTemplate<String, CommerceDomainEvent>
    commerceEventKafkaTemplate(
            ProducerFactory<String, CommerceDomainEvent>
                    commerceEventProducerFactory
    ) {

        return new KafkaTemplate<>(commerceEventProducerFactory);
    }

    /**
     * Declares the Commerce events topic.
     *
     * KafkaAdmin can create this topic when it connects
     * to the Kafka cluster and topic creation is permitted.
     */
    @Bean
    NewTopic commerceEventsTopic(
            @Value("${commerce.events.topic:commerce.events}")
            String topic
    ) {

        // Topic configuration:
        // - Configurable topic name.
        // - Three partitions.
        // - One replica.
        return TopicBuilder.name(topic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
