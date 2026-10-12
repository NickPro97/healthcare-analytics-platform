import type { RiskAssessment } from "../api/types";
import { featureLabel, formatNumber, percent } from "../utils/format";

export function RiskCard({ risk }: { risk: RiskAssessment }) {
  const maxAbs = Math.max(0.0001, ...risk.topFactors.map((f) => Math.abs(f.contribution)));
  return (
    <div>
      <div className="risk-head">
        <div className={`risk-score risk-${risk.riskLevel.toLowerCase()}`}>{percent(risk.riskScore)}</div>
        <div>
          <div><strong>{risk.riskLevel}</strong> estimated in-hospital mortality risk</div>
          <div className="small muted">
            From {risk.observations} observations in the first 24h of the latest admission · model {risk.modelVersion}
          </div>
        </div>
      </div>
      <h3 className="small-head">What pushed the score</h3>
      <ul className="factors">
        {risk.topFactors.map((f) => (
          <li key={f.feature}>
            <span className="factor-name">{featureLabel(f.feature)}</span>
            <span className="factor-value mono">{f.value === null ? "missing" : formatNumber(f.value, 1)}</span>
            <span className="factor-bar">
              <span
                className={f.contribution >= 0 ? "up" : "down"}
                style={{ width: `${(Math.abs(f.contribution) / maxAbs) * 100}%` }}
                title={`${f.contribution >= 0 ? "+" : ""}${f.contribution.toFixed(3)} log-odds`}
              />
            </span>
          </li>
        ))}
      </ul>
      <p className="small muted">
        Demonstration model trained on the 100-patient MIMIC-III demo; its accuracy is close to chance. Not for clinical decisions.
      </p>
    </div>
  );
}
