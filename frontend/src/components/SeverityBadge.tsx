import type { Severity } from "../api/types";
import { SEVERITY_LABEL } from "../utils/format";

export function SeverityBadge({ severity, score }: { severity: Severity | "MEDIUM" | "HIGH" | "LOW"; score?: number }) {
  return (
    <span className={`badge badge-${severity.toLowerCase()}`} title="NEWS2 clinical risk band">
      {SEVERITY_LABEL[severity as Severity] ?? severity}
      {score !== undefined && <strong> · {score}</strong>}
    </span>
  );
}
