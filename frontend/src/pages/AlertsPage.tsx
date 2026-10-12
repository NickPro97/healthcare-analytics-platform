import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useAsync } from "../hooks/useAsync";
import { SeverityBadge } from "../components/SeverityBadge";
import { Empty, ErrorNote, Loading } from "../components/States";
import { formatDateTime } from "../utils/format";
import { useLiveAlerts } from "../components/Layout";

type Filter = "OPEN" | "ACKNOWLEDGED" | "ALL";

export function AlertsPage() {
  const [filter, setFilter] = useState<Filter>("OPEN");
  const { data, error, loading, reload, setData } = useAsync(
    () => api.alerts({ status: filter === "ALL" ? undefined : filter, limit: 200 }), [filter]);
  const [ackError, setAckError] = useState<unknown>(null);
  const { alerts: live } = useLiveAlerts();
  useEffect(() => { if (live.length) reload(); }, [live.length, reload]);

  async function acknowledge(id: number) {
    setAckError(null);
    try {
      const updated = await api.acknowledge(id);
      setData((prev) => (prev ?? []).flatMap((a) => (a.id !== id ? [a] : filter === "OPEN" ? [] : [updated])));
    } catch (e) {
      setAckError(e);
    }
  }

  return (
    <div className="page">
      <header className="page-head row">
        <div>
          <h1>Alerts</h1>
          <p className="muted">Raised when NEWS2 reaches a red score, 5 or more, or 7 or more.</p>
        </div>
        <div className="segmented" role="tablist">
          {(["OPEN", "ACKNOWLEDGED", "ALL"] as Filter[]).map((f) => (
            <button key={f} role="tab" aria-selected={filter === f} className={filter === f ? "on" : ""} onClick={() => setFilter(f)}>
              {f.charAt(0) + f.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </header>
      {error ? <ErrorNote error={error} /> : null}
      {ackError ? <ErrorNote error={ackError} /> : null}
      <div className="card">
        {loading && !data ? <Loading /> : !data?.length ? <Empty>No alerts here.</Empty> : (
          <table className="table">
            <thead><tr><th>Severity</th><th>Patient</th><th>Reasons</th><th>Raised</th><th></th></tr></thead>
            <tbody>
              {data.map((a) => (
                <tr key={a.id}>
                  <td><SeverityBadge severity={a.severity} score={a.news2Score} /></td>
                  <td><Link to={`/patients/${a.patientId}`}>{a.patientName ?? `Patient ${a.patientId}`}</Link><div className="small muted">{a.mrn}</div></td>
                  <td className="small">{a.reasons}</td>
                  <td className="small">{formatDateTime(a.createdAt)}</td>
                  <td>
                    {a.status === "OPEN"
                      ? <button className="btn btn-small" onClick={() => void acknowledge(a.id)}>Acknowledge</button>
                      : <span className="small muted">by {a.acknowledgedBy}</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
