package com.nikhil.analytics;

import com.nikhil.analytics.alert.Alert;
import com.nikhil.analytics.alert.AlertService;
import com.nikhil.analytics.alert.PatientScoreRepository;
import com.nikhil.analytics.events.AlertCreatedEvent;
import com.nikhil.analytics.events.AlertPublisher;
import com.nikhil.analytics.events.VitalSignRecordedEvent;
import com.nikhil.analytics.scoring.RiskLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AlertFlowIntegrationTest {

    private static final AtomicLong PATIENT_IDS = new AtomicLong(1000);

    @Autowired
    private AlertService alertService;

    @Autowired
    private PatientScoreRepository scoreRepository;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AlertPublisher alertPublisher;

    @Test
    void deterioratingPatientRaisesAndPublishesAlert() {
        long pid = PATIENT_IDS.incrementAndGet();
        Optional<Alert> alert = alertService.process(event(pid, LocalDateTime.of(2026, 1, 1, 10, 0), 26.0, 92.0, 95.0, 125.0, 38.6));

        assertThat(alert).isPresent();
        assertThat(alert.get().getSeverity()).isEqualTo(RiskLevel.HIGH);
        assertThat(alert.get().getNews2Score()).isEqualTo(10);
        verify(alertPublisher, times(1)).publish(any(AlertCreatedEvent.class));
        assertThat(scoreRepository.findById(pid)).get().extracting(s -> s.getNews2Score()).isEqualTo(10);
    }

    @Test
    void stablePatientUpdatesScoreButNoAlert() {
        long pid = PATIENT_IDS.incrementAndGet();
        assertThat(alertService.process(event(pid, LocalDateTime.now(), 16.0, 98.0, 125.0, 72.0, 36.8))).isEmpty();
        verify(alertPublisher, never()).publish(any());
        assertThat(scoreRepository.findById(pid)).get().extracting(s -> s.getLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void duplicateEventIsIgnored() {
        long pid = PATIENT_IDS.incrementAndGet();
        VitalSignRecordedEvent e = event(pid, LocalDateTime.now(), 30.0, 85.0, 80.0, 140.0, 40.0);
        assertThat(alertService.process(e)).isPresent();
        assertThat(alertService.process(e)).isEmpty();
        verify(alertPublisher, times(1)).publish(any());
    }

    @Test
    void olderEventDoesNotOverwriteLatestScore() {
        long pid = PATIENT_IDS.incrementAndGet();
        alertService.process(event(pid, LocalDateTime.of(2026, 1, 1, 12, 0), 16.0, 98.0, 125.0, 72.0, 36.8)); // score 0
        alertService.process(event(pid, LocalDateTime.of(2026, 1, 1, 8, 0), 30.0, 85.0, 80.0, 140.0, 40.0));  // older, bad
        assertThat(scoreRepository.findById(pid)).get().extracting(s -> s.getNews2Score()).isEqualTo(0);
    }

    @Test
    void apiRequiresJwtAndSupportsAcknowledge() throws Exception {
        long pid = PATIENT_IDS.incrementAndGet();
        Alert alert = alertService.process(event(pid, LocalDateTime.now(), 26.0, 92.0, 95.0, 125.0, 38.6)).orElseThrow();
        String token = TestTokens.token("clinician", "CLINICIAN");

        mvc.perform(get("/api/alerts")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/alerts").param("patientId", String.valueOf(pid)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(alert.getId()))
                .andExpect(jsonPath("$[0].status").value("OPEN"));

        mvc.perform(post("/api/alerts/" + alert.getId() + "/acknowledge").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.acknowledgedBy").value("clinician"));

        mvc.perform(post("/api/alerts/999999/acknowledge").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/analytics/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientsMonitored").isNumber());
    }

    private static VitalSignRecordedEvent event(long patientId, LocalDateTime at, Double rr, Double spo2,
                                                Double sbp, Double hr, Double temp) {
        return new VitalSignRecordedEvent(UUID.randomUUID().toString(), patientId, "Patient " + patientId,
                "MRN-" + patientId, 1L, at, hr, sbp, 70.0, rr, spo2, temp, Instant.now());
    }
}
