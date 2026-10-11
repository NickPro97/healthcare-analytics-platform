package com.nikhil.patientservice.controller;

import com.nikhil.patientservice.entity.Admission;
import com.nikhil.patientservice.entity.Patient;
import com.nikhil.patientservice.entity.VitalSign;
import com.nikhil.patientservice.risk.RiskAssessment;
import com.nikhil.patientservice.risk.RiskService;
import com.nikhil.patientservice.service.AdmissionService;
import com.nikhil.patientservice.service.PatientService;
import com.nikhil.patientservice.service.VitalSignService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final AdmissionService admissionService;
    private final VitalSignService vitalSignService;
    private final RiskService riskService;

    public PatientController(PatientService patientService,
                             AdmissionService admissionService,
                             VitalSignService vitalSignService,
                             RiskService riskService) {
        this.patientService = patientService;
        this.admissionService = admissionService;
        this.vitalSignService = vitalSignService;
        this.riskService = riskService;
    }

    // ---------- patients ----------

    /** GET /api/patients, optional ?q= (name or MRN contains) or ?mrn= (exact). */
    @GetMapping
    public List<Patient> getPatients(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) String mrn) {
        if (mrn != null) {
            return patientService.getPatientByMrn(mrn).map(List::of).orElse(List.of());
        }
        return patientService.search(q);
    }

    @GetMapping("/stats")
    public PatientService.PlatformStats stats() {
        return patientService.stats();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        return patientService.getPatientById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Patient> createPatient(@Valid @RequestBody Patient patient) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.createPatient(patient));
    }

    @PutMapping("/{id}")
    public Patient updatePatient(@PathVariable Long id, @Valid @RequestBody Patient patientDetails) {
        return patientService.updatePatient(id, patientDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- admissions ----------

    @GetMapping("/{id}/admissions")
    public List<Admission> admissions(@PathVariable Long id) {
        return admissionService.list(id);
    }

    @PostMapping("/{id}/admissions")
    public ResponseEntity<Admission> addAdmission(@PathVariable Long id, @Valid @RequestBody Admission admission) {
        return ResponseEntity.status(HttpStatus.CREATED).body(admissionService.create(id, admission));
    }

    // ---------- vital signs ----------

    @GetMapping("/{id}/vitals")
    public List<VitalSign> vitals(@PathVariable Long id, @RequestParam(defaultValue = "200") int limit) {
        return vitalSignService.latest(id, limit);
    }

    /** Live observation: stored and streamed to Kafka for early-warning scoring. */
    @PostMapping("/{id}/vitals")
    public ResponseEntity<VitalSign> addVital(@PathVariable Long id, @Valid @RequestBody VitalSign vital) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vitalSignService.record(id, vital));
    }

    /** Historical bulk import (no events). */
    @PostMapping("/{id}/vitals/batch")
    public ResponseEntity<Map<String, Integer>> importVitals(@PathVariable Long id,
                                                             @RequestBody List<@Valid VitalSign> vitals) {
        int saved = vitalSignService.importBatch(id, vitals);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("imported", saved));
    }

    // ---------- ML risk ----------

    @GetMapping("/{id}/risk")
    public RiskAssessment risk(@PathVariable Long id) {
        return riskService.assess(id);
    }
}
