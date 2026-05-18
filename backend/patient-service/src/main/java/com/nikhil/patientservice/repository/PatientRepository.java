package com.nikhil.patientservice.repository;

import com.nikhil.patientservice.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}
