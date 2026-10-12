package com.nikhil.patientservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "admissions", indexes = @Index(name = "ix_admissions_patient", columnList = "patientId"))
public class Admission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;

    /** External encounter id, e.g. MIMIC HADM_ID. */
    @Size(max = 40)
    private String externalId;

    @NotNull
    private LocalDateTime admitTime;

    private LocalDateTime dischargeTime;

    @Size(max = 40)
    private String admissionType;

    @Size(max = 255)
    private String diagnosis;

    /** True if the patient died in hospital during this admission (MIMIC HOSPITAL_EXPIRE_FLAG). */
    private Boolean diedInHospital;

    public Admission() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public LocalDateTime getAdmitTime() { return admitTime; }
    public void setAdmitTime(LocalDateTime admitTime) { this.admitTime = admitTime; }
    public LocalDateTime getDischargeTime() { return dischargeTime; }
    public void setDischargeTime(LocalDateTime dischargeTime) { this.dischargeTime = dischargeTime; }
    public String getAdmissionType() { return admissionType; }
    public void setAdmissionType(String admissionType) { this.admissionType = admissionType; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public Boolean getDiedInHospital() { return diedInHospital; }
    public void setDiedInHospital(Boolean diedInHospital) { this.diedInHospital = diedInHospital; }
}
