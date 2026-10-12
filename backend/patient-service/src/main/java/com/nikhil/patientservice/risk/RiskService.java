package com.nikhil.patientservice.risk;

import com.nikhil.patientservice.entity.Admission;
import com.nikhil.patientservice.entity.Patient;
import com.nikhil.patientservice.entity.VitalSign;
import com.nikhil.patientservice.exception.PatientNotFoundException;
import com.nikhil.patientservice.repository.AdmissionRepository;
import com.nikhil.patientservice.repository.PatientRepository;
import com.nikhil.patientservice.repository.VitalSignRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;

/**
 * Builds the model input the same way the model was trained:
 * the first 24 hours of charted vitals in the patient's latest admission
 * (or the last 24 hours of vitals if no admission is on file).
 */
@Service
public class RiskService {

    static final int WINDOW_HOURS = 24;
    static final int MAX_AGE = 90; // MIMIC shifts ages above 89; cap like the training data

    private final PatientRepository patientRepository;
    private final AdmissionRepository admissionRepository;
    private final VitalSignRepository vitalSignRepository;
    private final MlClient mlClient;

    public RiskService(PatientRepository patientRepository,
                       AdmissionRepository admissionRepository,
                       VitalSignRepository vitalSignRepository,
                       MlClient mlClient) {
        this.patientRepository = patientRepository;
        this.admissionRepository = admissionRepository;
        this.vitalSignRepository = vitalSignRepository;
        this.mlClient = mlClient;
    }

    @Cacheable(cacheNames = "risk", key = "#patientId")
    public RiskAssessment assess(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        Optional<Admission> admission = admissionRepository.findFirstByPatientIdOrderByAdmitTimeDesc(patientId);

        LocalDateTime windowStart = null;
        if (admission.isPresent()) {
            windowStart = vitalSignRepository
                    .findFirstByPatientIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(patientId, admission.get().getAdmitTime())
                    .map(VitalSign::getRecordedAt)
                    .orElse(null);
        }
        if (windowStart == null) {
            windowStart = vitalSignRepository.findFirstByPatientIdOrderByRecordedAtDesc(patientId)
                    .map(v -> v.getRecordedAt().minusHours(WINDOW_HOURS))
                    .orElse(null);
        }

        List<VitalSign> window = windowStart == null
                ? List.of()
                : vitalSignRepository.findByPatientIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                        patientId, windowStart, windowStart.plusHours(WINDOW_HOURS));

        LocalDateTime ageReference = admission.map(Admission::getAdmitTime)
                .orElse(window.isEmpty() ? null : window.get(0).getRecordedAt());

        MlPredictRequest request = new MlPredictRequest(
                ageAt(patient, ageReference),
                patient.getGender(),
                window.stream().map(v -> new MlPredictRequest.Vital(
                        v.getHeartRate(), v.getSystolicBp(), v.getDiastolicBp(),
                        v.getRespiratoryRate(), v.getSpo2(), v.getTemperatureC())).toList());

        MlPredictResponse prediction = mlClient.predict(request);

        return new RiskAssessment(
                patientId,
                prediction.riskScore(),
                prediction.riskLevel(),
                prediction.modelVersion(),
                windowStart,
                windowStart == null ? null : windowStart.plusHours(WINDOW_HOURS),
                prediction.observations(),
                prediction.topFactors() == null ? List.of() : prediction.topFactors(),
                Instant.now());
    }

    static Double ageAt(Patient patient, LocalDateTime reference) {
        if (patient.getDateOfBirth() == null || reference == null) {
            return null;
        }
        int years = Period.between(patient.getDateOfBirth(), reference.toLocalDate()).getYears();
        return (double) Math.max(0, Math.min(MAX_AGE, years));
    }
}
