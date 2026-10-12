package com.nikhil.analytics.alert;

import com.nikhil.analytics.scoring.RiskLevel;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;

/** Latest NEWS2 per patient, used by the "who is deteriorating right now" dashboard. */
@Entity
@Table(name = "patient_scores")
public class PatientScore {

    @Id
    private Long patientId;

    private String patientName;
    private String mrn;
    private int news2Score;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RiskLevel level;

    @Column(length = 200)
    private String missingParameters;

    private LocalDateTime recordedAt;
    private Instant updatedAt;

    public PatientScore() {
    }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getMrn() { return mrn; }
    public void setMrn(String mrn) { this.mrn = mrn; }
    public int getNews2Score() { return news2Score; }
    public void setNews2Score(int news2Score) { this.news2Score = news2Score; }
    public RiskLevel getLevel() { return level; }
    public void setLevel(RiskLevel level) { this.level = level; }
    public String getMissingParameters() { return missingParameters; }
    public void setMissingParameters(String missingParameters) { this.missingParameters = missingParameters; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
