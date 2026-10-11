package com.nikhil.analytics.scoring;

import java.util.List;
import java.util.Map;

/**
 * @param total       aggregate score of the parameters that were measured
 * @param components  per-parameter score, only for measured parameters
 * @param missing     parameters not present in this observation (scored as 0)
 * @param level       NEWS2 clinical risk band
 * @param reasons     human-readable reasons, e.g. "Respiratory rate 26/min (3)"
 */
public record News2Result(int total, Map<String, Integer> components, List<String> missing,
                          RiskLevel level, List<String> reasons) {

    public boolean requiresAlert() {
        return level != RiskLevel.LOW;
    }
}
