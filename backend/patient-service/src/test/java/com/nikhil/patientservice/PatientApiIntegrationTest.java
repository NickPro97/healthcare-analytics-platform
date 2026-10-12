package com.nikhil.patientservice;

import com.nikhil.patientservice.risk.MlClient;
import com.nikhil.patientservice.risk.MlPredictRequest;
import com.nikhil.patientservice.risk.MlPredictResponse;
import com.nikhil.patientservice.risk.MlServiceUnavailableException;
import com.nikhil.patientservice.risk.RiskFactor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end through the real Spring context: security filter chain, JWT, validation,
 * JPA (H2 in PostgreSQL mode) and error handling. Only the ML HTTP client is mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PatientApiIntegrationTest {

    private static final Pattern TOKEN = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MlClient mlClient;

    private String adminToken;
    private String clinicianToken;

    @BeforeEach
    void login() throws Exception {
        adminToken = loginAs("nikhil", "admin-pass");
        clinicianToken = loginAs("clinician", "clinician-pass");
    }

    // ---------- auth ----------

    @Test
    void loginReturnsTokenAndRole() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nikhil\",\"password\":\"admin-pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void legacyLoginPathStillWorks() throws Exception {
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"clinician\",\"password\":\"clinician-pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CLINICIAN"));
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nikhil\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsNeedToken() throws Exception {
        mvc.perform(get("/api/patients")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/patients").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // ---------- patients ----------

    @Test
    void patientCrudFlow() throws Exception {
        String mrn = uniqueMrn();
        long id = createPatient("Test Patient", mrn, "F", "1950-04-02");

        mvc.perform(get("/api/patients/" + id).header("Authorization", bearer(clinicianToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mrn").value(mrn))
                .andExpect(jsonPath("$.dateOfBirth").value("1950-04-02"));

        mvc.perform(get("/api/patients").param("mrn", mrn).header("Authorization", bearer(clinicianToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(put("/api/patients/" + id).header("Authorization", bearer(clinicianToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientJson("Renamed Patient", mrn, "F", "1950-04-02")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Patient"));

        // clinicians cannot delete, admins can
        mvc.perform(delete("/api/patients/" + id).header("Authorization", bearer(clinicianToken)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/patients/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/patients/" + id).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateMrnIs409() throws Exception {
        String mrn = uniqueMrn();
        createPatient("First", mrn, "M", "1960-01-01");
        mvc.perform(post("/api/patients").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientJson("Second", mrn, "M", "1960-01-01")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate MRN"));
    }

    @Test
    void invalidPatientIs400() throws Exception {
        mvc.perform(post("/api/patients").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"mrn\":\"X1\",\"gender\":\"Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void unknownPatientIs404() throws Exception {
        mvc.perform(get("/api/patients/999999/vitals").header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    // ---------- admissions + vitals ----------

    @Test
    void admissionsAndVitalsFlow() throws Exception {
        long id = createPatient("Vitals Patient", uniqueMrn(), "M", "1940-06-15");

        mvc.perform(post("/api/patients/" + id + "/admissions").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"H1\",\"admitTime\":\"2026-01-01T08:00:00\",\"admissionType\":\"EMERGENCY\",\"diagnosis\":\"SEPSIS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(id));

        mvc.perform(post("/api/patients/" + id + "/vitals").header("Authorization", bearer(clinicianToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedAt\":\"2026-01-01T09:00:00\",\"heartRate\":118,\"systolicBp\":92,\"respiratoryRate\":24,\"spo2\":93,\"temperatureC\":38.4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.heartRate").value(118.0));

        mvc.perform(post("/api/patients/" + id + "/vitals/batch").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"recordedAt\":\"2026-01-01T10:00:00\",\"heartRate\":110},"
                                + "{\"recordedAt\":\"2026-01-01T11:00:00\",\"heartRate\":104,\"spo2\":95}]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imported").value(2));

        mvc.perform(get("/api/patients/" + id + "/vitals").header("Authorization", bearer(clinicianToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].recordedAt").value("2026-01-01T09:00:00"))   // oldest first
                .andExpect(jsonPath("$[2].recordedAt").value("2026-01-01T11:00:00"));
    }

    @Test
    void impossibleVitalIs400() throws Exception {
        long id = createPatient("Bad Vitals", uniqueMrn(), "F", "1970-01-01");
        mvc.perform(post("/api/patients/" + id + "/vitals").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedAt\":\"2026-01-01T09:00:00\",\"spo2\":140}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/patients/" + id + "/vitals").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedAt\":\"2026-01-01T09:00:00\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- risk ----------

    @Test
    void riskUsesFirst24hOfLatestAdmissionAndCapsAge() throws Exception {
        long id = createPatient("Risk Patient", uniqueMrn(), "F", "1800-01-01"); // MIMIC-style shifted DOB
        mvc.perform(post("/api/patients/" + id + "/admissions").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"admitTime\":\"2026-02-01T00:00:00\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/patients/" + id + "/vitals/batch").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"recordedAt\":\"2026-02-01T02:00:00\",\"heartRate\":120},"
                                + "{\"recordedAt\":\"2026-02-01T20:00:00\",\"heartRate\":100},"
                                + "{\"recordedAt\":\"2026-02-03T08:00:00\",\"heartRate\":80}]"))   // outside 24h window
                .andExpect(status().isCreated());

        when(mlClient.predict(any())).thenReturn(new MlPredictResponse(
                0.42, "HIGH", "test-model", 2, List.of(new RiskFactor("heart_rate_max", 120.0, 0.8))));

        mvc.perform(get("/api/patients/" + id + "/risk").header("Authorization", bearer(clinicianToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").value(0.42))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.windowStart").value("2026-02-01T02:00:00"))
                .andExpect(jsonPath("$.topFactors[0].feature").value("heart_rate_max"));

        ArgumentCaptor<MlPredictRequest> captor = ArgumentCaptor.forClass(MlPredictRequest.class);
        verify(mlClient).predict(captor.capture());
        assertThat(captor.getValue().vitals()).hasSize(2);
        assertThat(captor.getValue().age()).isEqualTo(90.0);
        assertThat(captor.getValue().gender()).isEqualTo("F");
    }

    @Test
    void riskReturns503WhenMlServiceDown() throws Exception {
        long id = createPatient("No ML", uniqueMrn(), "M", "1980-01-01");
        when(mlClient.predict(any())).thenThrow(new MlServiceUnavailableException("down", null));
        mvc.perform(get("/api/patients/" + id + "/risk").header("Authorization", bearer(adminToken)))
                .andExpect(status().isServiceUnavailable());
    }

    // ---------- helpers ----------

    private String loginAs(String user, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Matcher m = TOKEN.matcher(result.getResponse().getContentAsString());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private long createPatient(String name, String mrn, String gender, String dob) throws Exception {
        MvcResult result = mvc.perform(post("/api/patients").header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientJson(name, mrn, gender, dob)))
                .andExpect(status().isCreated())
                .andReturn();
        Matcher m = ID.matcher(result.getResponse().getContentAsString());
        assertThat(m.find()).isTrue();
        return Long.parseLong(m.group(1));
    }

    private static String patientJson(String name, String mrn, String gender, String dob) {
        return "{\"name\":\"" + name + "\",\"mrn\":\"" + mrn + "\",\"gender\":\"" + gender + "\",\"dateOfBirth\":\"" + dob + "\"}";
    }

    private static String uniqueMrn() {
        return "T-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
