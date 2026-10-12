"""Replay vital signs as if they were arriving live, to drive Kafka -> NEWS2 -> alerts -> browser.

    # replay real MIMIC demo sequences for 5 patients, one observation every 2 seconds
    python -m pipeline.simulate_stream --data ~/Desktop/patient-health-analytics/data --patients 5

    # no dataset needed: one patient who slowly deteriorates (sepsis-like picture)
    python -m pipeline.simulate_stream --scenario deteriorating

Each observation is posted with recordedAt = now, so it is treated as live data.
"""
from __future__ import annotations

import argparse
import itertools
import random
import time
import re
from datetime import datetime, timedelta, timezone
from pathlib import Path

from pipeline.api import PatientApi, default_credentials
from pipeline.ingest_mimic import mrn_for, vital_rows

DEMO_MRN = "DEMO-DETERIORATING-1"


def deteriorating_series(steps: int = 20, seed: int = 7) -> list[dict]:
    """Stable -> septic over `steps` observations (illustrative, not real patient data)."""
    rng = random.Random(seed)
    out = []
    for i in range(steps):
        f = i / max(steps - 1, 1)
        out.append({
            "heartRate": round(78 + 52 * f + rng.uniform(-3, 3), 1),
            "systolicBp": round(128 - 40 * f + rng.uniform(-4, 4), 1),
            "diastolicBp": round(76 - 22 * f + rng.uniform(-3, 3), 1),
            "respiratoryRate": round(15 + 13 * f + rng.uniform(-1, 1), 1),
            "spo2": round(98 - 7 * f + rng.uniform(-0.5, 0.5), 1),
            "temperatureC": round(36.9 + 2.3 * f + rng.uniform(-0.1, 0.1), 1),
        })
    return out


# Containers run on UTC; scripts/simulate.sh passes the host's offset (e.g. "-0400") so
# recordedAt matches the clinician's wall clock, like a real bedside monitor.
TZ_OFFSET = timedelta(0)


def parse_offset(text: str) -> timedelta:
    m = re.fullmatch(r"([+-])(\d{2}):?(\d{2})", text.strip())
    if not m:
        raise ValueError(f"bad UTC offset {text!r}, expected like -0400")
    sign = -1 if m.group(1) == "-" else 1
    return sign * timedelta(hours=int(m.group(2)), minutes=int(m.group(3)))


def now_iso() -> str:
    return (datetime.now(timezone.utc) + TZ_OFFSET).replace(microsecond=0, tzinfo=None).isoformat()


def run_scenario(api: PatientApi, interval: float) -> None:
    patient = api.find_by_mrn(DEMO_MRN) or api.create_patient(
        {"name": "Demo Deteriorating Patient", "mrn": DEMO_MRN, "gender": "F", "dateOfBirth": "1958-03-14"})
    for v in deteriorating_series():
        saved = api.record_vital(patient["id"], {"recordedAt": now_iso(), **v})
        print(f"{patient['mrn']}: {v}  -> vital #{saved['id']}")
        time.sleep(interval)


def run_mimic(api: PatientApi, data_dir: Path, n_patients: int, interval: float) -> None:
    from training import mimic_extract as mx
    vitals = mx.load_vitals(data_dir)
    admissions = mx.load_admissions(data_dir)
    # prefer admissions that ended in death: they are more likely to show deterioration
    subjects = list(admissions.sort_values("hospital_expire_flag", ascending=False)["subject_id"].unique())
    streams = []
    for subject in subjects:
        patient = api.find_by_mrn(mrn_for(subject))
        if patient is None:
            continue  # run pipeline.ingest_mimic first
        rows = vital_rows(vitals[vitals["subject_id"] == subject].tail(60))
        streams.append((patient, rows))
        if len(streams) == n_patients:
            break
    if not streams:
        raise SystemExit("No MIMIC patients found in the platform. Run: python -m pipeline.ingest_mimic --data ...")
    for batch in itertools.zip_longest(*[[(p, r) for r in rows] for p, rows in streams]):
        for item in batch:
            if item is None:
                continue
            patient, row = item
            row = {**row, "recordedAt": now_iso()}
            if all(row[k] is None for k in row if k != "recordedAt"):
                continue
            api.record_vital(patient["id"], row)
            print(f"{patient['mrn']}: HR={row['heartRate']} SBP={row['systolicBp']} RR={row['respiratoryRate']} "
                  f"SpO2={row['spo2']} T={row['temperatureC']}")
            time.sleep(interval)


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--api", default="http://localhost:8080")
    ap.add_argument("--scenario", choices=["mimic", "deteriorating"], default="mimic")
    ap.add_argument("--data", type=Path)
    ap.add_argument("--patients", type=int, default=5)
    ap.add_argument("--interval", type=float, default=2.0, help="seconds between observations")
    ap.add_argument("--utc-offset", default="+0000", help="local time zone offset, e.g. -0400")
    args = ap.parse_args()

    global TZ_OFFSET
    TZ_OFFSET = parse_offset(args.utc_offset)

    api = PatientApi(args.api)
    api.login(*default_credentials())
    if args.scenario == "deteriorating":
        run_scenario(api, args.interval)
    else:
        if not args.data:
            ap.error("--data is required for --scenario mimic")
        run_mimic(api, args.data.expanduser(), args.patients, args.interval)


if __name__ == "__main__":
    main()
