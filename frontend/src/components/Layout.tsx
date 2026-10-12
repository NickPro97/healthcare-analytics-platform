import { createContext, useContext, useEffect, useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useAlertStream, type StreamStatus } from "../hooks/useAlertStream";
import type { LiveAlert } from "../api/types";
import { SeverityBadge } from "./SeverityBadge";

interface LiveValue {
  alerts: LiveAlert[];
  status: StreamStatus;
}

const LiveContext = createContext<LiveValue>({ alerts: [], status: "offline" });
export const useLiveAlerts = () => useContext(LiveContext);

export function Layout() {
  const { session, logout } = useAuth();
  const { alerts, status, latest } = useAlertStream(session?.token ?? null);
  const [toast, setToast] = useState<LiveAlert | null>(null);

  useEffect(() => {
    if (!latest) return;
    setToast(latest);
    const t = window.setTimeout(() => setToast(null), 8000);
    return () => window.clearTimeout(t);
  }, [latest]);

  return (
    <LiveContext.Provider value={{ alerts, status }}>
      <div className="shell">
        <aside className="sidebar">
          <div className="brand">
            <img src="/favicon.svg" alt="" width={28} height={28} />
            <span>Healthcare Analytics</span>
          </div>
          <nav>
            <NavLink to="/" end>Dashboard</NavLink>
            <NavLink to="/patients">Patients</NavLink>
            <NavLink to="/alerts">Alerts</NavLink>
            <NavLink to="/assistant">AI Assistant</NavLink>
          </nav>
          <div className="sidebar-foot">
            <div className={`live live-${status}`} title="Real-time alert stream">
              <span className="dot" /> {status === "live" ? "Live" : status === "connecting" ? "Connecting" : "Offline"}
            </div>
            <div className="user">
              {session?.username} <span className="muted">({session?.role.toLowerCase()})</span>
            </div>
            <button className="btn btn-ghost" onClick={logout}>Sign out</button>
          </div>
        </aside>
        <main className="content">
          <Outlet />
        </main>
        {toast && (
          <div className="toast" role="alert" onClick={() => setToast(null)}>
            <SeverityBadge severity={toast.severity} score={toast.news2Score} />
            <div>
              <strong>{toast.patientName ?? `Patient ${toast.patientId}`}</strong>
              <div className="small">{toast.reasons}</div>
            </div>
          </div>
        )}
      </div>
    </LiveContext.Provider>
  );
}
