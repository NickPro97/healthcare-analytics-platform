package com.nikhil.patientservice.risk;

import java.util.List;

public record MlPredictRequest(Double age, String gender, List<Vital> vitals) {

    public record Vital(Double heartRate, Double systolicBp, Double diastolicBp,
                        Double respiratoryRate, Double spo2, Double temperatureC) {
    }
}
