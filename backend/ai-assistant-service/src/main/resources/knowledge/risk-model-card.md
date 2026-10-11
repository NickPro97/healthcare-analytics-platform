# ML risk model - model card

## Purpose
Estimates the probability of in-hospital death for an admission using only the first 24 hours of charted vital signs plus age and sex. It is a demonstration of an end-to-end ML pipeline (training, serving, monitoring) and is not a validated clinical tool.

## Features
For heart rate, systolic BP, diastolic BP, respiratory rate, SpO2 and temperature: the mean, minimum and maximum over the first 24 hours of charted vitals. Plus age (capped at 90) and male sex. Missing values are filled with the training median, and all features are standardised.

## Algorithm
Logistic regression with balanced class weights. It was chosen over a gradient-boosted tree model because the per-feature contributions are directly interpretable: each factor shown in the UI is the coefficient multiplied by the standardised value (its push on the log-odds).

## Evaluation
Performance is measured with stratified 5-fold cross-validation (ROC AUC). The exact numbers for the trained model are reported by the ML service at /model/info. Because the demo data is tiny, the confidence interval is wide.

## Risk levels
LOW below 10%, MEDIUM 10-30%, HIGH 30% or more. These thresholds are illustrative, not calibrated for any hospital.

## Known limitations
Small training set; single centre; data from 2001-2012; no laboratory values, medications or diagnoses as inputs; dates are shifted. The model can be confidently wrong and must not drive clinical decisions.
