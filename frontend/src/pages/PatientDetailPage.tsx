import { useParams, Link } from "react-router-dom";
import { api } from "../api/client";
import { useAsync } from "../hooks/useAsync";
import { VitalsChart } from "../components/VitalsChart";
import { RiskCard } from "../components/RiskCard";
import { AskPanel } from "../components/AskPanel";
import { SeverityBadge } from "../components/SeverityBadge";
import { Empty, ErrorNote, Loading } from "../components/States";
import { ageAt, formatDateTime, formatNumber } from "../utils/format";
import { useLiveAlerts } from "../components/Layout";
import { useEffect } from "react";
import type { VitalSign } from "../api/types";

type VitalKey = "heartRate" | "systolicBp" | "diastolicBp" | "respiratoryRate" | "spo2" | "temperatureC";

/** Most recent non-empty value of one vital. Real charting is sparse, so each tile has its own time. */
export function latestOf(vitals: VitalSign[] | null, key: VitalKey): { value: number; at: string } | null {
  if (!vitals) return null;
  for (let i = vitals.length - 1; i >= 0; i--) {
    const v = vitals[i][key];
    if (v !== null && v !== undefined) return { value: v, at: vitals[i].recordedAt };
  }
  return null;
}

function fmtLatest(latest: { value: number; at: string } | null, digits: number) {
  return latest ? { text: formatNumber(latest.value, digits), at: latest.at } : { text: "-", at: null as string | null };
}

/** Systolic and diastolic are only shown together when they were measured at the same time. */
function bloodPressure(vitals: VitalSign[] | null) {
  const sys = latestOf(vitals, "systolicBp");
  if (!sys || !vitals) return { text: "-", at: null as string | null };
  const sameTime = vitals.find((v) => v.recordedAt === sys.at && v.diastolicBp !== null);
  const dia = sameTime?.diastolicBp;
  return { text: dia !== null && dia !== undefined ? `${formatNumber(sys.value)}/${formatNumber(dia)}` : formatNumber(sys.value), at: sys.at };
}

export function PatientDetailPage() {
  const id = Number(useParams().id);
  const patient = useAsync(() => api.patient(id), [id]);
  const admissions = useAsync(() => api.admissions(id), [id]);
  const vitals = useAsync(() => api.vitals(id, 300), [id]);
  const risk = useAsync(() => api.risk(id), [id]);
  const alerts = useAsync(() => api.alerts({ patientId: id, limit: 20 }), [id]);
  const { alerts: live } = useLiveAlerts();

  // a live alert for this patient means new vitals arrived: refresh
  const newestLiveForPatient = live.find((a) => a.patientId === id)?.alertId;
  const { reload: reloadVitals } = vitals;
  const { reload: reloadAlerts } = alerts;
  useEffect(() => {
    if (newestLiveForPatient) { reloadVitals(); reloadAlerts(); }
  }, [newestLiveForPatient, reloadVitals, reloadAlerts]);

  if (patient.loading && !patient.data) return <Loading />;
  if (patient.error || !patient.data) return <div className="page"><ErrorNote error={patient.error ?? "Patient not found"} /></div>;

  const p = patient.data;
  const latestAdmission = admissions.data?.[0] ?? null;

  return (
    <div className="page">
      <header className="page-head">
        <Link to="/patients" className="small">&larr; Patients</Link>
        <h1>{p.name}</h1>
        <p className="muted">
          <span className="mono">{p.mrn}</span> · sex {p.gender ?? "-"} · age {ageAt(p.dateOfBirth, latestAdmission?.admitTime ?? null) ?? "-"} at latest admission
        </p>
      </header>

      <section className="grid stats-grid">
        {[
          { label: "Heart rate", unit: "/min", value: fmtLatest(latestOf(vitals.data, "heartRate"), 0) },
          { label: "Blood pressure", unit: "mmHg", value: bloodPressure(vitals.data) },
          { label: "Resp. rate", unit: "/min", value: fmtLatest(latestOf(vitals.data, "respiratoryRate"), 0) },
          { label: "SpO2", unit: "%", value: fmtLatest(latestOf(vitals.data, "spo2"), 0) },
          { label: "Temperature", unit: "°C", value: fmtLatest(latestOf(vitals.data, "temperatureC"), 1) },
        ].map((tile) => (
          <div className="card stat" key={tile.label}>
            <div className="stat-label">{tile.label}</div>
            <div className="stat-value">
              {tile.value.text}
              {tile.value.at && <span className="unit"> {tile.unit}</span>}
            </div>
            <div className="stat-hint">{tile.value.at ? formatDateTime(tile.value.at) : "not recorded"}</div>
          </div>
        ))}
      </section>

      <section className="card">
        <h2>Vital signs</h2>
        {vitals.loading && !vitals.data ? <Loading /> : vitals.error ? <ErrorNote error={vitals.error} /> :
          !vitals.data?.length ? <Empty>No vital signs recorded.</Empty> : <VitalsChart vitals={vitals.data} />}
      </section>

      <section className="grid two">
        <div className="card">
          <h2>ML risk score</h2>
          {risk.loading && !risk.data ? <Loading label="Scoring..." /> : risk.error ? <ErrorNote error={risk.error} /> :
            risk.data ? <RiskCard risk={risk.data} /> : null}
        </div>
        <div className="card">
          <h2>Early-warning alerts</h2>
          {alerts.error ? <ErrorNote error={alerts.error} /> : !alerts.data?.length ? <Empty>No alerts for this patient.</Empty> : (
            <ul className="feed">
              {alerts.data.map((a) => (
                <li key={a.id}>
                  <SeverityBadge severity={a.severity} score={a.news2Score} />
                  <div>
                    <div className="small">{a.reasons}</div>
                    <div className="small muted">{formatDateTime(a.createdAt)} · {a.status.toLowerCase()}</div>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <section className="grid two">
        <div className="card">
          <h2>Admissions</h2>
          {!admissions.data?.length ? <Empty>No admissions on file.</Empty> : (
            <table className="table">
              <thead><tr><th>Admitted</th><th>Type</th><th>Diagnosis</th><th>Outcome</th></tr></thead>
              <tbody>
                {admissions.data.map((a) => (
                  <tr key={a.id}>
                    <td className="small">{formatDateTime(a.admitTime)}</td>
                    <td className="small">{a.admissionType ?? "-"}</td>
                    <td className="small">{a.diagnosis ?? "-"}</td>
                    <td className="small">{a.diedInHospital === null ? "-" : a.diedInHospital ? "Died in hospital" : "Discharged"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        <div className="card">
          <h2>Ask the AI assistant</h2>
          <AskPanel patientId={id} suggestions={[
            "Summarise this patient's recent vital signs",
            "Do these vitals meet qSOFA criteria?",
            "Why might NEWS2 and the ML score disagree?",
          ]} />
        </div>
      </section>
    </div>
  );
}
