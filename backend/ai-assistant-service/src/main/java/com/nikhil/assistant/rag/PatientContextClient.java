package com.nikhil.assistant.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Fetches a short, de-identified summary of one patient from patient-service, forwarding
 * the caller's own JWT so the assistant can never see more than the user can.
 */
@Component
public class PatientContextClient {

    private final RestClient restClient;

    public PatientContextClient(RestClient.Builder builder, @Value("${assistant.patient-service-url}") String baseUrl) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(2));
        rf.setReadTimeout(Duration.ofSeconds(8));
        this.restClient = builder.baseUrl(baseUrl).requestFactory(rf).build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PatientDto(Long id, String gender, String dateOfBirth) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VitalDto(String recordedAt, Double heartRate, Double systolicBp, Double diastolicBp,
                           Double respiratoryRate, Double spo2, Double temperatureC) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RiskDto(Double riskScore, String riskLevel, String modelVersion, Integer observations) {
    }

    /** Returns a plain-text summary, or empty if the patient cannot be loaded. */
    public Optional<String> summarize(Long patientId, String authorizationHeader) {
        try {
            PatientDto patient = restClient.get().uri("/api/patients/{id}", patientId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve().body(PatientDto.class);
            if (patient == null) {
                return Optional.empty();
            }
            VitalDto[] vitals = restClient.get().uri("/api/patients/{id}/vitals?limit=12", patientId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve().body(VitalDto[].class);

            RiskDto risk = null;
            try {
                risk = restClient.get().uri("/api/patients/{id}/risk", patientId)
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .retrieve().body(RiskDto.class);
            } catch (RestClientException ignored) {
                // risk model is optional context
            }
            return Optional.of(format(patient, vitals == null ? List.of() : List.of(vitals), risk));
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }

    static String format(PatientDto p, List<VitalDto> vitals, RiskDto risk) {
        StringBuilder sb = new StringBuilder();
        sb.append("Patient #").append(p.id()).append(", sex ").append(p.gender() == null ? "unknown" : p.gender()).append('\n');
        if (risk != null && risk.riskScore() != null) {
            sb.append(String.format(Locale.ROOT, "ML in-hospital mortality risk (first 24h vitals): %.0f%% (%s, model %s)%n",
                    risk.riskScore() * 100, risk.riskLevel(), risk.modelVersion()));
        }
        if (vitals.isEmpty()) {
            sb.append("No vital signs on file.\n");
        } else {
            sb.append("Most recent vital signs (oldest to newest):\n");
            for (VitalDto v : vitals) {
                sb.append("- ").append(v.recordedAt()).append(": ")
                        .append(fmt("HR", v.heartRate())).append(fmt("SBP", v.systolicBp()))
                        .append(fmt("DBP", v.diastolicBp())).append(fmt("RR", v.respiratoryRate()))
                        .append(fmt("SpO2", v.spo2())).append(fmt("Temp C", v.temperatureC())).append('\n');
            }
        }
        return sb.toString();
    }

    private static String fmt(String label, Double value) {
        return value == null ? "" : String.format(Locale.ROOT, "%s %.1f; ", label, value);
    }
}
