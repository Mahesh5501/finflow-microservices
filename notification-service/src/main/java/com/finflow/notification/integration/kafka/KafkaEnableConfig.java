package com.finflow.notification.integration.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "notification.kafka-enabled", havingValue = "true")
public class KafkaEnableConfig {
}
