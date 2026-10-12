package com.nikhil.patientservice.repository;

import com.nikhil.patientservice.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByMrn(String mrn);

    boolean existsByMrn(String mrn);

    List<Patient> findByNameContainingIgnoreCaseOrMrnContainingIgnoreCaseOrderByIdAsc(String name, String mrn);
}
