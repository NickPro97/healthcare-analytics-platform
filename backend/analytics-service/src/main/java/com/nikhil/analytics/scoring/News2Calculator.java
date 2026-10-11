package com.nikhil.analytics.scoring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * National Early Warning Score 2 (NEWS2), using the five physiological parameters this
 * platform records: respiration rate, SpO2 (scale 1), systolic BP, pulse and temperature.
 *
 * Not included (not captured by the data model): level of consciousness (ACVPU) and
 * supplemental oxygen. The score is therefore a partial NEWS2 and can under-estimate risk.
 * It is decision support only, not a diagnosis.
 */
public final class News2Calculator {

    private News2Calculator() {
    }

    public static News2Result score(Double respiratoryRate, Double spo2, Double systolicBp,
                                    Double heartRate, Double temperatureC) {
        Map<String, Integer> components = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        add("respiratoryRate", respiratoryRate, respiration(respiratoryRate), "Respiratory rate %.0f/min", components, missing, reasons);
        add("spo2", spo2, spo2Scale1(spo2), "SpO2 %.0f%%", components, missing, reasons);
        add("systolicBp", systolicBp, systolic(systolicBp), "Systolic BP %.0f mmHg", components, missing, reasons);
        add("heartRate", heartRate, pulse(heartRate), "Pulse %.0f/min", components, missing, reasons);
        add("temperatureC", temperatureC, temperature(temperatureC), "Temperature %.1f C", components, missing, reasons);

        int total = components.values().stream().mapToInt(Integer::intValue).sum();
        boolean red = components.values().stream().anyMatch(s -> s == 3);

        RiskLevel level;
        if (total >= 7) {
            level = RiskLevel.HIGH;
        } else if (total >= 5) {
            level = RiskLevel.MEDIUM;
        } else if (red) {
            level = RiskLevel.LOW_MEDIUM;
        } else {
            level = RiskLevel.LOW;
        }
        return new News2Result(total, components, missing, level, reasons);
    }

    private static void add(String name, Double value, int points, String label,
                            Map<String, Integer> components, List<String> missing, List<String> reasons) {
        if (value == null) {
            missing.add(name);
            return;
        }
        components.put(name, points);
        if (points > 0) {
            reasons.add(String.format(java.util.Locale.ROOT, label, value) + " (+" + points + ")");
        }
    }

    // Each band follows the NEWS2 chart. Values are compared with <= upper bounds so that
    // decimals between bands (e.g. RR 8.5) fall into the next band, as charted values are rounded.

    static int respiration(Double rr) {
        if (rr == null) return 0;
        if (rr <= 8) return 3;
        if (rr <= 11) return 1;
        if (rr <= 20) return 0;
        if (rr <= 24) return 2;
        return 3;
    }

    static int spo2Scale1(Double s) {
        if (s == null) return 0;
        if (s <= 91) return 3;
        if (s <= 93) return 2;
        if (s <= 95) return 1;
        return 0;
    }

    static int systolic(Double sbp) {
        if (sbp == null) return 0;
        if (sbp <= 90) return 3;
        if (sbp <= 100) return 2;
        if (sbp <= 110) return 1;
        if (sbp <= 219) return 0;
        return 3;
    }

    static int pulse(Double hr) {
        if (hr == null) return 0;
        if (hr <= 40) return 3;
        if (hr <= 50) return 1;
        if (hr <= 90) return 0;
        if (hr <= 110) return 1;
        if (hr <= 130) return 2;
        return 3;
    }

    static int temperature(Double t) {
        if (t == null) return 0;
        if (t <= 35.0) return 3;
        if (t <= 36.0) return 1;
        if (t <= 38.0) return 0;
        if (t <= 39.0) return 1;
        return 2;
    }
}
