import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useAsync } from "../hooks/useAsync";
import { Empty, ErrorNote, Loading } from "../components/States";

export function PatientsPage() {
  const [query, setQuery] = useState("");
  const [debounced, setDebounced] = useState("");
  useEffect(() => {
    const t = window.setTimeout(() => setDebounced(query.trim()), 250);
    return () => window.clearTimeout(t);
  }, [query]);

  const { data, error, loading } = useAsync(() => api.patients(debounced || undefined), [debounced]);

  return (
    <div className="page">
      <header className="page-head row">
        <div>
          <h1>Patients</h1>
          <p className="muted">{data ? `${data.length} shown` : " "}</p>
        </div>
        <input className="search" placeholder="Search name or MRN" value={query} onChange={(e) => setQuery(e.target.value)} aria-label="Search patients" />
      </header>
      {error ? <ErrorNote error={error} /> : null}
      <div className="card">
        {loading && !data ? <Loading /> : !data?.length ? <Empty>No patients found. Run the MIMIC ingest script to load the demo.</Empty> : (
          <table className="table">
            <thead><tr><th>Name</th><th>MRN</th><th>Sex</th><th>Date of birth (shifted)</th></tr></thead>
            <tbody>
              {data.map((p) => (
                <tr key={p.id}>
                  <td><Link to={`/patients/${p.id}`}>{p.name}</Link></td>
                  <td className="mono">{p.mrn}</td>
                  <td>{p.gender ?? "-"}</td>
                  <td className="small muted">{p.dateOfBirth ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
