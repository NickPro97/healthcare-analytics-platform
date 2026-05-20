package com.nikhil.patientservice.service;

import com.nikhil.patientservice.entity.Patient;
import com.nikhil.patientservice.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository)  {
        this.patientRepository = patientRepository;
    }

    public List<Patient> getAllPatients()   {
        return patientRepository.findAll();
    }
}
