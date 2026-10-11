package com.nikhil.patientservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * One set of bedside observations taken at the same time. Every measurement is optional
 * because real charting is sparse (MIMIC rarely records all six at the same minute).
 * Ranges reject physically impossible values (typos, unit errors) but keep extreme real ones.
 */
@Entity
@Table(name = "vital_signs", indexes = @Index(name = "ix_vitals_patient_time", columnList = "patientId,recordedAt"))
public class VitalSign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;

    @NotNull
    private LocalDateTime recordedAt;

    @DecimalMin("0") @DecimalMax("300")
    private Double heartRate;

    @DecimalMin("0") @DecimalMax("300")
    private Double systolicBp;

    @DecimalMin("0") @DecimalMax("200")
    private Double diastolicBp;

    @DecimalMin("0") @DecimalMax("80")
    private Double respiratoryRate;

    @DecimalMin("0") @DecimalMax("100")
    private Double spo2;

    @DecimalMin("25") @DecimalMax("45")
    private Double temperatureC;

    public VitalSign() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public Double getHeartRate() { return heartRate; }
    public void setHeartRate(Double heartRate) { this.heartRate = heartRate; }
    public Double getSystolicBp() { return systolicBp; }
    public void setSystolicBp(Double systolicBp) { this.systolicBp = systolicBp; }
    public Double getDiastolicBp() { return diastolicBp; }
    public void setDiastolicBp(Double diastolicBp) { this.diastolicBp = diastolicBp; }
    public Double getRespiratoryRate() { return respiratoryRate; }
    public void setRespiratoryRate(Double respiratoryRate) { this.respiratoryRate = respiratoryRate; }
    public Double getSpo2() { return spo2; }
    public void setSpo2(Double spo2) { this.spo2 = spo2; }
    public Double getTemperatureC() { return temperatureC; }
    public void setTemperatureC(Double temperatureC) { this.temperatureC = temperatureC; }
}
