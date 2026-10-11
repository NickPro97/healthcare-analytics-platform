import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { VitalSign } from "../api/types";
import { formatDateTime } from "../utils/format";

const SERIES: { key: keyof VitalSign; name: string; color: string }[] = [
  { key: "heartRate", name: "Heart rate", color: "#dc2626" },
  { key: "systolicBp", name: "Systolic BP", color: "#2563eb" },
  { key: "respiratoryRate", name: "Resp. rate", color: "#0d9488" },
  { key: "spo2", name: "SpO2 %", color: "#7c3aed" },
];

export function VitalsChart({ vitals }: { vitals: VitalSign[] }) {
  const data = vitals.map((v) => ({ ...v, t: formatDateTime(v.recordedAt) }));
  return (
    <div className="chart" aria-label="Vital signs over time">
      <ResponsiveContainer width="100%" height={300}>
        <LineChart data={data} margin={{ top: 8, right: 16, left: -8, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--grid)" />
          <XAxis dataKey="t" minTickGap={48} tick={{ fontSize: 11, fill: "var(--muted)" }} />
          <YAxis tick={{ fontSize: 11, fill: "var(--muted)" }} />
          <Tooltip contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)", color: "var(--text)" }} />
          <Legend wrapperStyle={{ fontSize: 12 }} />
          {SERIES.map((s) => (
            <Line key={s.key} type="monotone" dataKey={s.key} name={s.name} stroke={s.color} dot={false} strokeWidth={1.8} connectNulls isAnimationActive={false} />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
