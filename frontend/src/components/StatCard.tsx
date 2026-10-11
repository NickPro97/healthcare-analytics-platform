export function StatCard({ label, value, hint, tone }: { label: string; value: string | number; hint?: string; tone?: "danger" | "warn" }) {
  return (
    <div className={`card stat ${tone ? `stat-${tone}` : ""}`}>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {hint && <div className="stat-hint">{hint}</div>}
    </div>
  );
}
