# Data source - MIMIC-III Clinical Database Demo

## What it is
MIMIC-III is a de-identified database of critical-care admissions at Beth Israel Deaconess Medical Center (Boston, USA) between 2001 and 2012, created by the MIT Laboratory for Computational Physiology. The demo subset used by this platform contains 100 patients and is openly available on PhysioNet under the Open Data Commons Open Database License.

## De-identification
Dates are shifted into the future (years around 2100-2200) consistently within a patient, so intervals are real but calendar dates are not. Patients older than 89 have their date of birth shifted so that their computed age appears to be about 300; the platform caps age at 90.

## Tables used
PATIENTS (sex, date of birth), ADMISSIONS (admit and discharge time, admission type, diagnosis text, in-hospital death flag), ICUSTAYS and CHARTEVENTS (bedside observations such as heart rate, blood pressure, respiratory rate, SpO2 and temperature).

## Important limitation
With only 100 patients the demo is suitable for engineering and demonstration, not for clinical conclusions. Any model trained on it has very wide uncertainty and must not be used for patient care.
