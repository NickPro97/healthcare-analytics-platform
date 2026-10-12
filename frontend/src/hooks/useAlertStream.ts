import { useEffect, useRef, useState } from "react";
import type { LiveAlert } from "../api/types";

export type StreamStatus = "connecting" | "live" | "offline";

/**
 * Subscribes to notification-service over Server-Sent Events.
 * EventSource reconnects by itself; we only track status and keep the newest alerts.
 */
export function useAlertStream(token: string | null, max = 30) {
  const [alerts, setAlerts] = useState<LiveAlert[]>([]);
  const [status, setStatus] = useState<StreamStatus>("connecting");
  const [latest, setLatest] = useState<LiveAlert | null>(null);
  const maxRef = useRef(max);

  useEffect(() => {
    if (!token || typeof EventSource === "undefined") {
      setStatus("offline");
      return;
    }
    const es = new EventSource(`/api/notifications/stream?token=${encodeURIComponent(token)}`);
    setStatus("connecting");
    es.addEventListener("connected", () => setStatus("live"));
    es.addEventListener("alert", (e) => {
      try {
        const alert = JSON.parse((e as MessageEvent).data) as LiveAlert;
        setAlerts((prev) => [alert, ...prev].slice(0, maxRef.current));
        setLatest(alert);
      } catch {
        /* ignore malformed message */
      }
    });
    es.onerror = () => setStatus(es.readyState === EventSource.CLOSED ? "offline" : "connecting");
    return () => es.close();
  }, [token]);

  return { alerts, status, latest };
}
