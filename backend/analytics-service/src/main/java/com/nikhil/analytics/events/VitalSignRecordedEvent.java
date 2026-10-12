package com.nikhil.analytics.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.time.LocalDateTime;

/** Mirror of the event published by patient-service on "vitals.recorded". */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VitalSignRecordedEvent(
        String eventId,
        Long patientId,
        String patientName,
        String mrn,
        Long vitalSignId,
        LocalDateTime recordedAt,
        Double heartRate,
        Double systolicBp,
        Double diastolicBp,
        Double respiratoryRate,
        Double spo2,
        Double temperatureC,
        Instant publishedAt) {
}
