import type { Severity } from "../api/types";

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "-";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString(undefined, { year: "numeric", month: "short", day: "2-digit", hour: "2-digit", minute: "2-digit" });
}

export function formatNumber(v: number | null | undefined, digits = 0): string {
  return v === null || v === undefined || Number.isNaN(v) ? "-" : v.toFixed(digits);
}

export function percent(p: number): string {
  return `${Math.round(p * 100)}%`;
}

/** Age in whole years at a reference date; MIMIC dates are shifted so we cap at 90 like the backend. */
export function ageAt(dob: string | null, at: string | null): number | null {
  if (!dob || !at) return null;
  const b = new Date(dob);
  const a = new Date(at);
  if (Number.isNaN(b.getTime()) || Number.isNaN(a.getTime())) return null;
  let years = a.getFullYear() - b.getFullYear();
  const beforeBirthday = a.getMonth() < b.getMonth() || (a.getMonth() === b.getMonth() && a.getDate() < b.getDate());
  if (beforeBirthday) years -= 1;
  return Math.max(0, Math.min(90, years));
}

export const SEVERITY_LABEL: Record<Severity, string> = {
  LOW: "Low",
  LOW_MEDIUM: "Low-medium",
  MEDIUM: "Medium",
  HIGH: "High",
};

/** Turns "heart_rate_max" into "Heart rate (max)". */
export function featureLabel(feature: string): string {
  const m = feature.match(/^(.*)_(mean|min|max)$/);
  const base = (m ? m[1] : feature).replace(/_/g, " ").replace("spo2", "SpO2").replace("bp", "BP").replace(" c", " (C)");
  const label = base.charAt(0).toUpperCase() + base.slice(1);
  return m ? `${label} (${m[2]})` : label;
}
