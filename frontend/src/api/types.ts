export type Role = "ADMIN" | "CLINICIAN";

export interface LoginResponse {
  token: string;
  username: string;
  role: Role;
  expiresInSeconds: number;
}

export interface Patient {
  id: number;
  name: string;
  dateOfBirth: string | null;
  gender: "M" | "F" | "O" | "U" | null;
  mrn: string;
}

export interface Admission {
  id: number;
  patientId: number;
  externalId: string | null;
  admitTime: string;
  dischargeTime: string | null;
  admissionType: string | null;
  diagnosis: string | null;
  diedInHospital: boolean | null;
}

export interface VitalSign {
  id: number;
  patientId: number;
  recordedAt: string;
  heartRate: number | null;
  systolicBp: number | null;
  diastolicBp: number | null;
  respiratoryRate: number | null;
  spo2: number | null;
  temperatureC: number | null;
}

export interface RiskFactor {
  feature: string;
  value: number | null;
  contribution: number;
}

export interface RiskAssessment {
  patientId: number;
  riskScore: number;
  riskLevel: "LOW" | "MEDIUM" | "HIGH";
  modelVersion: string;
  windowStart: string | null;
  windowEnd: string | null;
  observations: number;
  topFactors: RiskFactor[];
  assessedAt: string;
}

export type Severity = "LOW" | "LOW_MEDIUM" | "MEDIUM" | "HIGH";

export interface Alert {
  id: number;
  patientId: number;
  patientName: string | null;
  mrn: string | null;
  vitalSignId: number | null;
  recordedAt: string | null;
  news2Score: number;
  severity: Severity;
  reasons: string | null;
  status: "OPEN" | "ACKNOWLEDGED";
  acknowledgedBy: string | null;
  acknowledgedAt: string | null;
  createdAt: string;
}

/** Shape pushed by notification-service (AlertCreatedEvent). */
export interface LiveAlert {
  alertId: number;
  patientId: number;
  patientName: string | null;
  mrn: string | null;
  news2Score: number;
  severity: Severity;
  reasons: string | null;
  recordedAt: string | null;
  createdAt: string;
}

export interface PatientScore {
  patientId: number;
  patientName: string | null;
  mrn: string | null;
  news2Score: number;
  level: Severity;
  missingParameters: string | null;
  recordedAt: string | null;
  updatedAt: string;
}

export interface AnalyticsSummary {
  openAlerts: number;
  alertsLast24h: number;
  openBySeverity: Partial<Record<Severity, number>>;
  patientsMonitored: number;
  patientsMediumOrHigh: number;
  topPatients: PatientScore[];
}

export interface PlatformStats {
  patients: number;
  admissions: number;
  vitalSigns: number;
}

export interface AssistantAnswer {
  answer: string;
  sources: string[];
  usedPatientData: boolean;
  disclaimer: string;
}
