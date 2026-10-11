package com.nikhil.patientservice.risk;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/** Thin HTTP client for the Python FastAPI ml-service. */
@Component
public class MlClient {

    private final RestClient restClient;

    public MlClient(RestClient.Builder builder, @Value("${app.ml.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public MlPredictResponse predict(MlPredictRequest request) {
        try {
            MlPredictResponse response = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(MlPredictResponse.class);
            if (response == null) {
                throw new MlServiceUnavailableException("ML service returned an empty body", null);
            }
            return response;
        } catch (RestClientException e) {
            throw new MlServiceUnavailableException("ML service call failed: " + e.getMessage(), e);
        }
    }
}
