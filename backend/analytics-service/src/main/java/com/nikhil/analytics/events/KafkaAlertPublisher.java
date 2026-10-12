package com.nikhil.analytics.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(name = "app.events.kafka-enabled", havingValue = "true")
public class KafkaAlertPublisher implements AlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaAlertPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final String topic;

    public KafkaAlertPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper,
                               @Value("${app.events.alerts-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.topic = topic;
    }

    @Override
    public void publish(AlertCreatedEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.patientId()), jsonMapper.writeValueAsString(event))
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish alert {}", event.alertId(), ex);
                    }
                });
    }
}
