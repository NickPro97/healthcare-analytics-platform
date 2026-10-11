import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useAsync } from "../hooks/useAsync";
import { StatCard } from "../components/StatCard";
import { SeverityBadge } from "../components/SeverityBadge";
import { useLiveAlerts } from "../components/Layout";
import { Empty, ErrorNote, Loading } from "../components/States";
import { formatDateTime } from "../utils/format";
import { useEffect } from "react";

export function DashboardPage() {
  const stats = useAsync(() => api.stats(), []);
  const summary = useAsync(() => api.summary(), []);
  const { alerts: live, status } = useLiveAlerts();

  // refresh the summary whenever a new live alert arrives
  const { reload } = summary;
  useEffect(() => { if (live.length) reload(); }, [live.length, reload]);

  const s = summary.data;
  return (
    <div className="page">
      <header className="page-head">
        <h1>Dashboard</h1>
        <p className="muted">Who needs attention now, from live NEWS2 scoring of every new observation.</p>
      </header>

      <section className="grid stats-grid">
        <StatCard label="Patients" value={stats.data?.patients ?? "-"} hint={`${stats.data?.admissions ?? "-"} admissions`} />
        <StatCard label="Vital-sign records" value={stats.data?.vitalSigns.toLocaleString() ?? "-"} />
        <StatCard label="Open alerts" value={s?.openAlerts ?? "-"} tone={s && s.openAlerts > 0 ? "warn" : undefined}
          hint={`${s?.alertsLast24h ?? "-"} in last 24h`} />
        <StatCard label="Medium / high risk now" value={s?.patientsMediumOrHigh ?? "-"} tone={s && s.patientsMediumOrHigh > 0 ? "danger" : undefined}
          hint={`${s?.patientsMonitored ?? "-"} patients with live scores`} />
      </section>
      {(stats.error || summary.error) ? <ErrorNote error={stats.error ?? summary.error} /> : null}

      <section className="grid two">
        <div className="card">
          <h2>Highest NEWS2 right now</h2>
          {summary.loading && !s ? <Loading /> : !s?.topPatients.length ? (
            <Empty>No live scores yet. Start the stream simulator to see patients here.</Empty>
          ) : (
            <table className="table">
              <thead><tr><th>Patient</th><th>NEWS2</th><th>Last observation</th><th>Not measured</th></tr></thead>
              <tbody>
                {s.topPatients.map((p) => (
                  <tr key={p.patientId}>
                    <td><Link to={`/patients/${p.patientId}`}>{p.patientName ?? `Patient ${p.patientId}`}</Link><div className="small muted">{p.mrn}</div></td>
                    <td><SeverityBadge severity={p.level} score={p.news2Score} /></td>
                    <td className="small">{formatDateTime(p.recordedAt)}</td>
                    <td className="small muted">{p.missingParameters || "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="card">
          <h2>Live alert feed <span className={`live live-${status} inline`}><span className="dot" /></span></h2>
          {!live.length ? <Empty>Waiting for alerts... new ones appear here instantly.</Empty> : (
            <ul className="feed">
              {live.map((a) => (
                <li key={a.alertId}>
                  <SeverityBadge severity={a.severity} score={a.news2Score} />
                  <div>
                    <Link to={`/patients/${a.patientId}`}>{a.patientName ?? `Patient ${a.patientId}`}</Link>
                    <div className="small muted">{a.reasons}</div>
                  </div>
                  <span className="small muted">{formatDateTime(a.createdAt)}</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </div>
  );
}
