package com.nikhil.patientservice.events;

import com.nikhil.patientservice.entity.Patient;
import com.nikhil.patientservice.entity.VitalSign;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/** Message published to the "vitals.recorded" Kafka topic. The analytics service consumes it. */
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

    public static VitalSignRecordedEvent of(Patient patient, VitalSign v) {
        return new VitalSignRecordedEvent(
                UUID.randomUUID().toString(),
                patient.getId(), patient.getName(), patient.getMrn(),
                v.getId(), v.getRecordedAt(),
                v.getHeartRate(), v.getSystolicBp(), v.getDiastolicBp(),
                v.getRespiratoryRate(), v.getSpo2(), v.getTemperatureC(),
                Instant.now());
    }
}
