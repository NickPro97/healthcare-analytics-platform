package com.nikhil.patientservice.risk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlPredictResponse(double riskScore, String riskLevel, String modelVersion,
                                int observations, List<RiskFactor> topFactors) {
}
