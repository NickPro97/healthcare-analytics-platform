package com.nikhil.analytics.events;

import com.nikhil.analytics.alert.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(name = "app.events.kafka-enabled", havingValue = "true")
public class VitalsListener {

    private static final Logger log = LoggerFactory.getLogger(VitalsListener.class);

    private final AlertService alertService;
    private final JsonMapper jsonMapper;

    public VitalsListener(AlertService alertService, JsonMapper jsonMapper) {
        this.alertService = alertService;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = "${app.events.vitals-topic}", groupId = "analytics-service")
    public void onVitalSign(String payload) {
        VitalSignRecordedEvent event;
        try {
            event = jsonMapper.readValue(payload, VitalSignRecordedEvent.class);
        } catch (JacksonException e) {
            // A malformed message must not block the partition: log and skip it.
            log.error("Skipping malformed vitals event: {}", payload, e);
            return;
        }
        alertService.process(event);
    }
}
