package com.nikhil.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Every replica uses its own consumer group (random suffix) so each one receives every
 * alert and can push it to the browsers connected to that replica.
 */
@Component
@ConditionalOnProperty(name = "app.events.kafka-enabled", havingValue = "true")
public class AlertsListener {

    private final NotificationHub hub;

    public AlertsListener(NotificationHub hub) {
        this.hub = hub;
    }

    @KafkaListener(topics = "${app.events.alerts-topic}", groupId = "${app.events.group-id}")
    public void onAlert(String alertJson) {
        hub.broadcast(alertJson);
    }
}
