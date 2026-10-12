import { ageAt, featureLabel, formatNumber, percent } from "../utils/format";

describe("format utils", () => {
  it("labels model features for humans", () => {
    expect(featureLabel("heart_rate_max")).toBe("Heart rate (max)");
    expect(featureLabel("spo2_min")).toBe("SpO2 (min)");
    expect(featureLabel("systolic_bp_mean")).toBe("Systolic BP (mean)");
    expect(featureLabel("age")).toBe("Age");
  });

  it("caps MIMIC shifted ages at 90 and handles birthdays", () => {
    expect(ageAt("1800-01-01", "2130-01-01T00:00:00")).toBe(90);
    expect(ageAt("2060-06-15", "2130-06-14T10:00:00")).toBe(69);
    expect(ageAt("2060-06-15", "2130-06-15T10:00:00")).toBe(70);
    expect(ageAt(null, "2130-01-01")).toBeNull();
  });

  it("formats numbers and percents", () => {
    expect(formatNumber(null)).toBe("-");
    expect(formatNumber(37.26, 1)).toBe("37.3");
    expect(percent(0.426)).toBe("43%");
  });
});
