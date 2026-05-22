package com.nikhil.patientservice.service;

import com.nikhil.patientservice.entity.Patient;
import com.nikhil.patientservice.exception.PatientNotFoundException;
import com.nikhil.patientservice.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository)  {
        this.patientRepository = patientRepository;
    }

    public List<Patient> getAllPatients()   {
        return patientRepository.findAll();
    }

    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    public Patient createPatient(Patient patient) {
        return patientRepository.save(patient);
    }

    public Patient updatePatient(Long id, Patient patientDetails) {
        Patient existing = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));

        existing.setName(patientDetails.getName());
        existing.setDateOfBirth(patientDetails.getDateOfBirth());
        existing.setGender(patientDetails.getGender());
        existing.setMrn(patientDetails.getMrn());

        return patientRepository.save(existing);
    }

    public void deletePatient(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new PatientNotFoundException(id);
        }
        patientRepository.deleteById(id);
    }
}
