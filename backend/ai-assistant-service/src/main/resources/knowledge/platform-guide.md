# Platform guide - how the pieces fit together

## Services
patient-service stores patients, admissions and vital signs and issues login tokens. analytics-service listens to every new vital sign on Kafka, computes NEWS2 and raises alerts. notification-service pushes alerts to the browser in real time. ml-service (Python) serves the mortality-risk model. ai-assistant-service (this assistant) answers questions using these reference notes and, if a patient is selected, that patient's recent data.

## Reading an alert
Each alert shows the NEWS2 total, the risk band (LOW_MEDIUM, MEDIUM or HIGH) and the reasons, for example "Respiratory rate 26/min (+3)". Acknowledging an alert records who reviewed it and when; it does not change the patient's score.

## NEWS2 versus the ML risk score
NEWS2 reacts to the latest single set of observations and answers "does this patient need review now?". The ML score looks at the first 24 hours of the admission and answers "how sick did this admission look early on?". They measure different things and can disagree.
