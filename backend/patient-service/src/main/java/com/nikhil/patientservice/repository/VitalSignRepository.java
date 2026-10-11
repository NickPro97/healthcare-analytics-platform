package com.nikhil.patientservice.repository;

import com.nikhil.patientservice.entity.VitalSign;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VitalSignRepository extends JpaRepository<VitalSign, Long> {

    List<VitalSign> findByPatientIdOrderByRecordedAtDesc(Long patientId, Pageable pageable);

    List<VitalSign> findByPatientIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long patientId, LocalDateTime from, LocalDateTime to);

    Optional<VitalSign> findFirstByPatientIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
            Long patientId, LocalDateTime from);

    Optional<VitalSign> findFirstByPatientIdOrderByRecordedAtDesc(Long patientId);

    long countByPatientId(Long patientId);

    @Transactional
    void deleteByPatientId(Long patientId);
}
