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
  const latest = vitals.data?.[vitals.data.length - 1];

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
          ["Heart rate", latest?.heartRate, "/min", 0],
          ["Blood pressure", latest ? `${formatNumber(latest.systolicBp)}/${formatNumber(latest.diastolicBp)}` : null, "mmHg", 0],
          ["Resp. rate", latest?.respiratoryRate, "/min", 0],
          ["SpO2", latest?.spo2, "%", 0],
          ["Temperature", latest?.temperatureC, "°C", 1],
        ].map(([label, value, unit, digits]) => (
          <div className="card stat" key={label as string}>
            <div className="stat-label">{label as string}</div>
            <div className="stat-value">
              {typeof value === "number" ? formatNumber(value, digits as number) : (value as string | null) ?? "-"}
              <span className="unit"> {unit as string}</span>
            </div>
            <div className="stat-hint">{latest ? formatDateTime(latest.recordedAt) : "no data"}</div>
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
