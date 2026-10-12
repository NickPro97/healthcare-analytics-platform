package com.nikhil.analytics.alert;

import com.nikhil.analytics.scoring.RiskLevel;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts",
        uniqueConstraints = @UniqueConstraint(name = "uk_alerts_event", columnNames = "sourceEventId"),
        indexes = {
                @Index(name = "ix_alerts_patient", columnList = "patientId"),
                @Index(name = "ix_alerts_created", columnList = "createdAt")
        })
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kafka event id - makes processing idempotent (Kafka delivers at least once). */
    @Column(nullable = false, length = 64)
    private String sourceEventId;

    private Long patientId;
    private String patientName;
    private String mrn;
    private Long vitalSignId;
    private LocalDateTime recordedAt;

    private int news2Score;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RiskLevel severity;

    @Column(length = 500)
    private String reasons;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AlertStatus status = AlertStatus.OPEN;

    private String acknowledgedBy;
    private Instant acknowledgedAt;
    private Instant createdAt;

    public Alert() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(String sourceEventId) { this.sourceEventId = sourceEventId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getMrn() { return mrn; }
    public void setMrn(String mrn) { this.mrn = mrn; }
    public Long getVitalSignId() { return vitalSignId; }
    public void setVitalSignId(Long vitalSignId) { this.vitalSignId = vitalSignId; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public int getNews2Score() { return news2Score; }
    public void setNews2Score(int news2Score) { this.news2Score = news2Score; }
    public RiskLevel getSeverity() { return severity; }
    public void setSeverity(RiskLevel severity) { this.severity = severity; }
    public String getReasons() { return reasons; }
    public void setReasons(String reasons) { this.reasons = reasons; }
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    public String getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
