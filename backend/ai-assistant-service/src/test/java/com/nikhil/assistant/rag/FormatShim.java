package com.nikhil.assistant.rag;

import java.util.List;

/** Test-only access to package-private formatting. */
public final class FormatShim {

    private FormatShim() {
    }

    public static String format() {
        return PatientContextClient.format(
                new PatientContextClient.PatientDto(3L, "M", "2100-01-01"),
                List.of(new PatientContextClient.VitalDto("2100-05-01T10:00:00", 110.0, null, null, null, null, null)),
                new PatientContextClient.RiskDto(0.42, "HIGH", "m1", 10));
    }
}
