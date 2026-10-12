import { latestOf } from "../pages/PatientDetailPage";
import type { VitalSign } from "../api/types";

const v = (recordedAt: string, partial: Partial<VitalSign>): VitalSign => ({
  id: 1, patientId: 1, recordedAt, heartRate: null, systolicBp: null, diastolicBp: null,
  respiratoryRate: null, spo2: null, temperatureC: null, ...partial,
});

describe("latestOf", () => {
  const vitals = [
    v("2164-10-25T06:00:00", { spo2: 97, temperatureC: 37.2, systolicBp: 140 }),
    v("2164-10-25T08:30:00", { heartRate: 66, respiratoryRate: 14 }),
  ];

  it("returns the newest reading of each vital, with its own time", () => {
    expect(latestOf(vitals, "heartRate")).toEqual({ value: 66, at: "2164-10-25T08:30:00" });
    expect(latestOf(vitals, "spo2")).toEqual({ value: 97, at: "2164-10-25T06:00:00" });
  });

  it("returns null when a vital was never recorded", () => {
    expect(latestOf(vitals, "diastolicBp")).toBeNull();
    expect(latestOf(null, "heartRate")).toBeNull();
  });
});
