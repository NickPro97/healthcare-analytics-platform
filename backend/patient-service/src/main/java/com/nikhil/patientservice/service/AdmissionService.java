package com.nikhil.patientservice.service;

import com.nikhil.patientservice.entity.Admission;
import com.nikhil.patientservice.repository.AdmissionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdmissionService {

    private final AdmissionRepository admissionRepository;
    private final PatientService patientService;

    public AdmissionService(AdmissionRepository admissionRepository, PatientService patientService) {
        this.admissionRepository = admissionRepository;
        this.patientService = patientService;
    }

    public List<Admission> list(Long patientId) {
        patientService.requirePatient(patientId);
        return admissionRepository.findByPatientIdOrderByAdmitTimeDesc(patientId);
    }

    @CacheEvict(cacheNames = "risk", key = "#patientId")
    public Admission create(Long patientId, Admission admission) {
        patientService.requirePatient(patientId);
        if (admission.getDischargeTime() != null && admission.getDischargeTime().isBefore(admission.getAdmitTime())) {
            throw new IllegalArgumentException("dischargeTime must not be before admitTime");
        }
        admission.setId(null);
        admission.setPatientId(patientId);
        return admissionRepository.save(admission);
    }
}
