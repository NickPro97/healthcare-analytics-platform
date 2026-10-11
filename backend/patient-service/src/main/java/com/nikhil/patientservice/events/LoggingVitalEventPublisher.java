package com.nikhil.patientservice.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Used when Kafka is switched off (unit tests, running the service alone). */
@Component
@ConditionalOnProperty(name = "app.events.kafka-enabled", havingValue = "false", matchIfMissing = true)
public class LoggingVitalEventPublisher implements VitalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingVitalEventPublisher.class);

    @Override
    public void publish(VitalSignRecordedEvent event) {
        log.debug("Kafka disabled - not publishing vital event {} for patient {}", event.eventId(), event.patientId());
    }
}
