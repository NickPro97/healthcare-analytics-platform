package com.nikhil.patientservice.risk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/** One feature's push on the risk score (log-odds contribution from the logistic model). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RiskFactor(String feature, Double value, double contribution) implements Serializable {
}
