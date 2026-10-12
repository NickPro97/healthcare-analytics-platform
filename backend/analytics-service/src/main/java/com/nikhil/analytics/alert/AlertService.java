package com.nikhil.analytics.alert;

import com.nikhil.analytics.events.AlertCreatedEvent;
import com.nikhil.analytics.events.AlertPublisher;
import com.nikhil.analytics.events.VitalSignRecordedEvent;
import com.nikhil.analytics.scoring.News2Calculator;
import com.nikhil.analytics.scoring.News2Result;
import com.nikhil.analytics.scoring.RiskLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    public static final int MAX_LIMIT = 500;

    private final AlertRepository alertRepository;
    private final PatientScoreRepository scoreRepository;
    private final AlertPublisher alertPublisher;

    public AlertService(AlertRepository alertRepository, PatientScoreRepository scoreRepository,
                        AlertPublisher alertPublisher) {
        this.alertRepository = alertRepository;
        this.scoreRepository = scoreRepository;
        this.alertPublisher = alertPublisher;
    }

    /**
     * Scores one observation, updates the patient's latest score, and raises an alert
     * when NEWS2 says the patient needs an urgent review. Duplicate events are ignored.
     *
     * @return the new alert, or empty if no alert was needed (or the event was a duplicate)
     */
    @Transactional
    public Optional<Alert> process(VitalSignRecordedEvent event) {
        if (event.eventId() == null || event.patientId() == null) {
            log.warn("Ignoring vitals event without id/patientId");
            return Optional.empty();
        }
        if (alertRepository.existsBySourceEventId(event.eventId())) {
            log.debug("Duplicate event {} ignored", event.eventId());
            return Optional.empty();
        }

        News2Result result = News2Calculator.score(event.respiratoryRate(), event.spo2(),
                event.systolicBp(), event.heartRate(), event.temperatureC());

        PatientScore score = scoreRepository.findById(event.patientId()).orElseGet(PatientScore::new);
        // Out-of-order events: only move the "latest" score forward in time.
        if (score.getRecordedAt() == null || event.recordedAt() == null
                || !event.recordedAt().isBefore(score.getRecordedAt())) {
            score.setPatientId(event.patientId());
            score.setPatientName(event.patientName());
            score.setMrn(event.mrn());
            score.setNews2Score(result.total());
            score.setLevel(result.level());
            score.setMissingParameters(String.join(",", result.missing()));
            score.setRecordedAt(event.recordedAt());
            score.setUpdatedAt(Instant.now());
            scoreRepository.save(score);
        }

        if (!result.requiresAlert()) {
            return Optional.empty();
        }

        Alert alert = new Alert();
        alert.setSourceEventId(event.eventId());
        alert.setPatientId(event.patientId());
        alert.setPatientName(event.patientName());
        alert.setMrn(event.mrn());
        alert.setVitalSignId(event.vitalSignId());
        alert.setRecordedAt(event.recordedAt());
        alert.setNews2Score(result.total());
        alert.setSeverity(result.level());
        alert.setReasons(truncate(String.join("; ", result.reasons()), 500));
        alert.setCreatedAt(Instant.now());
        Alert saved = alertRepository.save(alert);

        alertPublisher.publish(AlertCreatedEvent.of(saved));
        log.info("Alert {} raised for patient {}: NEWS2={} {}", saved.getId(), saved.getPatientId(),
                saved.getNews2Score(), saved.getSeverity());
        return Optional.of(saved);
    }

    public List<Alert> list(AlertStatus status, Long patientId, int limit) {
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(limit, MAX_LIMIT)));
        if (patientId != null) {
            return alertRepository.findByPatientIdOrderByCreatedAtDesc(patientId, page);
        }
        if (status != null) {
            return alertRepository.findByStatusOrderByCreatedAtDesc(status, page);
        }
        return alertRepository.findAllByOrderByCreatedAtDesc(page);
    }

    @Transactional
    public Alert acknowledge(Long id, String user) {
        Alert alert = alertRepository.findById(id).orElseThrow(() -> new AlertNotFoundException(id));
        if (alert.getStatus() == AlertStatus.OPEN) {
            alert.setStatus(AlertStatus.ACKNOWLEDGED);
            alert.setAcknowledgedBy(user);
            alert.setAcknowledgedAt(Instant.now());
        }
        return alert;
    }

    public List<PatientScore> latestScores() {
        return scoreRepository.findAllByOrderByNews2ScoreDescUpdatedAtDesc();
    }

    public Summary summary() {
        Map<RiskLevel, Long> openBySeverity = new EnumMap<>(RiskLevel.class);
        for (RiskLevel level : RiskLevel.values()) {
            if (level != RiskLevel.LOW) {
                openBySeverity.put(level, alertRepository.countBySeverityAndStatus(level, AlertStatus.OPEN));
            }
        }
        List<PatientScore> scores = latestScores();
        long highRisk = scores.stream().filter(s -> s.getLevel() == RiskLevel.HIGH || s.getLevel() == RiskLevel.MEDIUM).count();
        return new Summary(
                alertRepository.countByStatus(AlertStatus.OPEN),
                alertRepository.countByCreatedAtAfter(Instant.now().minus(Duration.ofHours(24))),
                openBySeverity,
                scores.size(),
                highRisk,
                scores.stream().limit(10).toList());
    }

    public record Summary(long openAlerts, long alertsLast24h, Map<RiskLevel, Long> openBySeverity,
                          long patientsMonitored, long patientsMediumOrHigh, List<PatientScore> topPatients) {
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
