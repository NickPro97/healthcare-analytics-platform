package com.nikhil.analytics.alert;

import com.nikhil.analytics.scoring.RiskLevel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    boolean existsBySourceEventId(String sourceEventId);

    List<Alert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status, Pageable pageable);

    List<Alert> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    long countByStatus(AlertStatus status);

    long countByCreatedAtAfter(Instant after);

    long countBySeverityAndStatus(RiskLevel severity, AlertStatus status);
}
