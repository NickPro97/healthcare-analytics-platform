package com.nikhil.patientservice.service;

import com.nikhil.patientservice.entity.Patient;
import com.nikhil.patientservice.exception.DuplicateMrnException;
import com.nikhil.patientservice.exception.PatientNotFoundException;
import com.nikhil.patientservice.repository.AdmissionRepository;
import com.nikhil.patientservice.repository.PatientRepository;
import com.nikhil.patientservice.repository.VitalSignRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AdmissionRepository admissionRepository;
    private final VitalSignRepository vitalSignRepository;

    public PatientService(PatientRepository patientRepository,
                          AdmissionRepository admissionRepository,
                          VitalSignRepository vitalSignRepository) {
        this.patientRepository = patientRepository;
        this.admissionRepository = admissionRepository;
        this.vitalSignRepository = vitalSignRepository;
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll(org.springframework.data.domain.Sort.by("id"));
    }

    public List<Patient> search(String query) {
        if (query == null || query.isBlank()) {
            return getAllPatients();
        }
        String q = query.trim();
        return patientRepository.findByNameContainingIgnoreCaseOrMrnContainingIgnoreCaseOrderByIdAsc(q, q);
    }

    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    public Optional<Patient> getPatientByMrn(String mrn) {
        return patientRepository.findByMrn(mrn);
    }

    public Patient requirePatient(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> new PatientNotFoundException(id));
    }

    public Patient createPatient(Patient patient) {
        if (patientRepository.existsByMrn(patient.getMrn())) {
            throw new DuplicateMrnException(patient.getMrn());
        }
        patient.setId(null);
        return patientRepository.save(patient);
    }

    @CacheEvict(cacheNames = "risk", key = "#id")
    public Patient updatePatient(Long id, Patient patientDetails) {
        Patient existing = requirePatient(id);

        if (!existing.getMrn().equals(patientDetails.getMrn()) && patientRepository.existsByMrn(patientDetails.getMrn())) {
            throw new DuplicateMrnException(patientDetails.getMrn());
        }
        existing.setName(patientDetails.getName());
        existing.setDateOfBirth(patientDetails.getDateOfBirth());
        existing.setGender(patientDetails.getGender());
        existing.setMrn(patientDetails.getMrn());

        return patientRepository.save(existing);
    }

    @Transactional
    @CacheEvict(cacheNames = "risk", key = "#id")
    public void deletePatient(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new PatientNotFoundException(id);
        }
        vitalSignRepository.deleteByPatientId(id);
        admissionRepository.deleteByPatientId(id);
        patientRepository.deleteById(id);
    }

    public PlatformStats stats() {
        return new PlatformStats(patientRepository.count(), admissionRepository.count(), vitalSignRepository.count());
    }

    public record PlatformStats(long patients, long admissions, long vitalSigns) {
    }
}
