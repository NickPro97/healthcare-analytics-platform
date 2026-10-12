package com.nikhil.patientservice.repository;

import com.nikhil.patientservice.entity.Admission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AdmissionRepository extends JpaRepository<Admission, Long> {

    List<Admission> findByPatientIdOrderByAdmitTimeDesc(Long patientId);

    Optional<Admission> findFirstByPatientIdOrderByAdmitTimeDesc(Long patientId);

    boolean existsByPatientIdAndExternalId(Long patientId, String externalId);

    @Transactional
    void deleteByPatientId(Long patientId);
}
