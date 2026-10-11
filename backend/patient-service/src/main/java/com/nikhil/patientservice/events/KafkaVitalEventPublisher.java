package com.nikhil.patientservice.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publishes vital-sign events as JSON strings. The key is the patient id, so all events
 * for one patient land on the same partition and are consumed in order.
 * A Kafka failure is logged, not thrown: saving the vital sign must not fail because the
 * event bus is down.
 */
@Component
@ConditionalOnProperty(name = "app.events.kafka-enabled", havingValue = "true")
public class KafkaVitalEventPublisher implements VitalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaVitalEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final String topic;

    public KafkaVitalEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                    JsonMapper jsonMapper,
                                    @Value("${app.events.vitals-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.topic = topic;
    }

    @Override
    public void publish(VitalSignRecordedEvent event) {
        String payload = jsonMapper.writeValueAsString(event);
        kafkaTemplate.send(topic, String.valueOf(event.patientId()), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish vital event {} for patient {}", event.eventId(), event.patientId(), ex);
                    }
                });
    }
}
