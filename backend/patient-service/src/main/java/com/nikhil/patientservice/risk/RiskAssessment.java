package com.nikhil.patientservice.risk;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Risk of in-hospital mortality estimated from the first 24 hours of charted vitals.
 * Serializable because it is cached in Redis.
 */
public record RiskAssessment(
        Long patientId,
        double riskScore,
        String riskLevel,
        String modelVersion,
        LocalDateTime windowStart,
        LocalDateTime windowEnd,
        int observations,
        List<RiskFactor> topFactors,
        Instant assessedAt) implements Serializable {
}
