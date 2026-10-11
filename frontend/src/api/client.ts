import type {
  Admission, Alert, AnalyticsSummary, AssistantAnswer, LoginResponse, Patient,
  PatientScore, PlatformStats, RiskAssessment, VitalSign,
} from "./types";

/** Error carrying the HTTP status and the problem+json "detail" when the server sent one. */
export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
    this.name = "ApiError";
  }
}

let authToken: string | null = null;
let onUnauthorized: (() => void) | null = null;

export function setAuthToken(token: string | null) {
  authToken = token;
}

export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler;
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  if (authToken) headers.set("Authorization", `Bearer ${authToken}`);

  const res = await fetch(path, { ...init, headers });
  if (res.status === 401 && authToken) onUnauthorized?.();
  if (!res.ok) {
    let message = `${res.status} ${res.statusText}`.trim();
    try {
      const body = await res.json();
      message = body.detail || body.title || message;
    } catch {
      /* body was not JSON */
    }
    throw new ApiError(res.status, message);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

const json = (body: unknown): RequestInit => ({ method: "POST", body: JSON.stringify(body) });

export const api = {
  login: (username: string, password: string) =>
    request<LoginResponse>("/api/auth/login", json({ username, password })),

  stats: () => request<PlatformStats>("/api/patients/stats"),
  patients: (q?: string) => request<Patient[]>(`/api/patients${q ? `?q=${encodeURIComponent(q)}` : ""}`),
  patient: (id: number) => request<Patient>(`/api/patients/${id}`),
  admissions: (id: number) => request<Admission[]>(`/api/patients/${id}/admissions`),
  vitals: (id: number, limit = 200) => request<VitalSign[]>(`/api/patients/${id}/vitals?limit=${limit}`),
  risk: (id: number) => request<RiskAssessment>(`/api/patients/${id}/risk`),
  recordVital: (id: number, vital: Partial<VitalSign>) => request<VitalSign>(`/api/patients/${id}/vitals`, json(vital)),

  alerts: (params: { status?: "OPEN" | "ACKNOWLEDGED"; patientId?: number; limit?: number } = {}) => {
    const qs = new URLSearchParams();
    if (params.status) qs.set("status", params.status);
    if (params.patientId) qs.set("patientId", String(params.patientId));
    qs.set("limit", String(params.limit ?? 50));
    return request<Alert[]>(`/api/alerts?${qs}`);
  },
  acknowledge: (id: number) => request<Alert>(`/api/alerts/${id}/acknowledge`, { method: "POST" }),
  summary: () => request<AnalyticsSummary>("/api/analytics/summary"),
  scores: () => request<PatientScore[]>("/api/analytics/scores"),

  ask: (question: string, patientId?: number) =>
    request<AssistantAnswer>("/api/assistant/ask", json({ question, patientId })),
};
