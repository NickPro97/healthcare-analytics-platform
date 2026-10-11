package com.nikhil.analytics.events;

import com.nikhil.analytics.alert.Alert;

import java.time.Instant;
import java.time.LocalDateTime;

/** Published on "alerts.created"; the notification service pushes it to browsers. */
public record AlertCreatedEvent(
        Long alertId,
        Long patientId,
        String patientName,
        String mrn,
        int news2Score,
        String severity,
        String reasons,
        LocalDateTime recordedAt,
        Instant createdAt) {

    public static AlertCreatedEvent of(Alert a) {
        return new AlertCreatedEvent(a.getId(), a.getPatientId(), a.getPatientName(), a.getMrn(),
                a.getNews2Score(), a.getSeverity().name(), a.getReasons(), a.getRecordedAt(), a.getCreatedAt());
    }
}
