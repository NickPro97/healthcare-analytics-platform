package com.nikhil.analytics.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientScoreRepository extends JpaRepository<PatientScore, Long> {

    List<PatientScore> findAllByOrderByNews2ScoreDescUpdatedAtDesc();
}
