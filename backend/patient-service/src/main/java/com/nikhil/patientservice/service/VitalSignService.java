package com.nikhil.patientservice.service;

import com.nikhil.patientservice.entity.Patient;
import com.nikhil.patientservice.entity.VitalSign;
import com.nikhil.patientservice.events.VitalEventPublisher;
import com.nikhil.patientservice.events.VitalSignRecordedEvent;
import com.nikhil.patientservice.repository.VitalSignRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class VitalSignService {

    public static final int MAX_LIMIT = 5000;
    public static final int MAX_BATCH = 5000;

    private final VitalSignRepository vitalSignRepository;
    private final PatientService patientService;
    private final VitalEventPublisher eventPublisher;

    public VitalSignService(VitalSignRepository vitalSignRepository,
                            PatientService patientService,
                            VitalEventPublisher eventPublisher) {
        this.vitalSignRepository = vitalSignRepository;
        this.patientService = patientService;
        this.eventPublisher = eventPublisher;
    }

    /** Latest {@code limit} observations, returned oldest-first so they can be charted directly. */
    public List<VitalSign> latest(Long patientId, int limit) {
        patientService.requirePatient(patientId);
        int size = Math.max(1, Math.min(limit, MAX_LIMIT));
        List<VitalSign> newestFirst = new ArrayList<>(
                vitalSignRepository.findByPatientIdOrderByRecordedAtDesc(patientId, PageRequest.of(0, size)));
        Collections.reverse(newestFirst);
        return newestFirst;
    }

    /** A live observation: saved, then published to Kafka for real-time early-warning scoring. */
    @CacheEvict(cacheNames = "risk", key = "#patientId")
    public VitalSign record(Long patientId, VitalSign vital) {
        Patient patient = patientService.requirePatient(patientId);
        requireAtLeastOneMeasurement(vital);
        vital.setId(null);
        vital.setPatientId(patientId);
        VitalSign saved = vitalSignRepository.save(vital);
        eventPublisher.publish(VitalSignRecordedEvent.of(patient, saved));
        return saved;
    }

    /** Historical bulk import (e.g. MIMIC demo). Not published: history should not raise live alerts. */
    @Transactional
    @CacheEvict(cacheNames = "risk", key = "#patientId")
    public int importBatch(Long patientId, List<VitalSign> vitals) {
        patientService.requirePatient(patientId);
        if (vitals.size() > MAX_BATCH) {
            throw new IllegalArgumentException("batch size must be <= " + MAX_BATCH);
        }
        for (VitalSign v : vitals) {
            requireAtLeastOneMeasurement(v);
            v.setId(null);
            v.setPatientId(patientId);
        }
        return vitalSignRepository.saveAll(vitals).size();
    }

    private static void requireAtLeastOneMeasurement(VitalSign v) {
        if (v.getHeartRate() == null && v.getSystolicBp() == null && v.getDiastolicBp() == null
                && v.getRespiratoryRate() == null && v.getSpo2() == null && v.getTemperatureC() == null) {
            throw new IllegalArgumentException("a vital sign record needs at least one measurement");
        }
    }
}
