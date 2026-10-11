# NEWS2 - National Early Warning Score 2

## What it is
NEWS2 is a bedside track-and-trigger score published by the Royal College of Physicians (London) in 2017. It adds points for how far each routine observation is from normal. A higher total means a higher chance of clinical deterioration (cardiac arrest, unplanned ICU admission or death) in the next 24 hours. It is meant to prompt a timely clinical review, not to diagnose.

## Parameters and points
Respiration rate (breaths/min): 8 or less = 3; 9-11 = 1; 12-20 = 0; 21-24 = 2; 25 or more = 3.

SpO2 scale 1 (%): 91 or less = 3; 92-93 = 2; 94-95 = 1; 96 or more = 0. Scale 2 is used only for patients with confirmed hypercapnic respiratory failure and a prescribed target of 88-92%.

Air or oxygen: any supplemental oxygen = 2; air = 0.

Systolic blood pressure (mmHg): 90 or less = 3; 91-100 = 2; 101-110 = 1; 111-219 = 0; 220 or more = 3.

Pulse (beats/min): 40 or less = 3; 41-50 = 1; 51-90 = 0; 91-110 = 1; 111-130 = 2; 131 or more = 3.

Consciousness: alert = 0; new confusion, or responds only to voice or pain, or unresponsive = 3.

Temperature (C): 35.0 or less = 3; 35.1-36.0 = 1; 36.1-38.0 = 0; 38.1-39.0 = 1; 39.1 or more = 2.

## Risk bands and recommended response
Aggregate 0: low risk, routine monitoring (at least every 12 hours).

Aggregate 1-4: low risk, ward-based response; monitoring at least every 4-6 hours and a registered nurse decides whether to escalate.

A score of 3 in any single parameter ("red score"): low-medium risk; urgent ward-based review by a clinician, monitoring at least hourly.

Aggregate 5-6: medium risk, key threshold for an urgent response; urgent review by a clinician competent in acute illness and consider sepsis; monitoring at least hourly.

Aggregate 7 or more: high risk, emergency response; emergency assessment by a team with critical-care skills, usually with transfer to a higher level of care; continuous monitoring.

## How this platform uses it
The analytics service computes NEWS2 for every vital-sign event using five parameters: respiration rate, SpO2 (scale 1), systolic BP, pulse and temperature. Consciousness level and supplemental oxygen are not captured, so the platform score is a partial NEWS2 and may under-estimate risk by up to 5 points. Missing observations score 0 and are listed on the alert. Alerts are raised for low-medium, medium and high risk.

## Limitations
NEWS2 is not validated for children under 16 or for pregnant women. It does not replace clinical judgement: a patient can be seriously ill with a low score (for example early sepsis, or a patient on rate-controlling drugs such as beta-blockers that blunt the heart-rate response).
