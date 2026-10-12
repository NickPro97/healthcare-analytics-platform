package com.nikhil.analytics.web;

import com.nikhil.analytics.alert.Alert;
import com.nikhil.analytics.alert.AlertService;
import com.nikhil.analytics.alert.AlertStatus;
import com.nikhil.analytics.alert.PatientScore;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/alerts")
    public List<Alert> alerts(@RequestParam(required = false) AlertStatus status,
                              @RequestParam(required = false) Long patientId,
                              @RequestParam(defaultValue = "50") int limit) {
        return alertService.list(status, patientId, limit);
    }

    @PostMapping("/alerts/{id}/acknowledge")
    public Alert acknowledge(@PathVariable Long id, Authentication authentication) {
        return alertService.acknowledge(id, authentication.getName());
    }

    @GetMapping("/analytics/scores")
    public List<PatientScore> scores() {
        return alertService.latestScores();
    }

    @GetMapping("/analytics/summary")
    public AlertService.Summary summary() {
        return alertService.summary();
    }
}
