import { render, screen } from "@testing-library/react";
import { SeverityBadge } from "../components/SeverityBadge";
import { RiskCard } from "../components/RiskCard";

describe("SeverityBadge", () => {
  it("shows band and score", () => {
    render(<SeverityBadge severity="LOW_MEDIUM" score={3} />);
    expect(screen.getByText(/Low-medium/)).toHaveClass("badge-low_medium");
    expect(screen.getByText(/3/)).toBeInTheDocument();
  });
});

describe("RiskCard", () => {
  it("renders score, level and factors including missing values", () => {
    render(
      <RiskCard
        risk={{
          patientId: 1, riskScore: 0.42, riskLevel: "HIGH", modelVersion: "logreg-test",
          windowStart: null, windowEnd: null, observations: 12, assessedAt: "2026-01-01T00:00:00Z",
          topFactors: [
            { feature: "heart_rate_max", value: 131, contribution: 0.8 },
            { feature: "spo2_min", value: null, contribution: -0.2 },
          ],
        }}
      />,
    );
    expect(screen.getByText("42%")).toBeInTheDocument();
    expect(screen.getByText("HIGH")).toBeInTheDocument();
    expect(screen.getByText("Heart rate (max)")).toBeInTheDocument();
    expect(screen.getByText("missing")).toBeInTheDocument();
    expect(screen.getByText(/12 observations/)).toBeInTheDocument();
  });
});
