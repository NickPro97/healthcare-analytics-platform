package com.nikhil.analytics.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.events.kafka-enabled", havingValue = "false", matchIfMissing = true)
public class LoggingAlertPublisher implements AlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingAlertPublisher.class);

    @Override
    public void publish(AlertCreatedEvent event) {
        log.info("Kafka disabled - alert {} for patient {} not published", event.alertId(), event.patientId());
    }
}
