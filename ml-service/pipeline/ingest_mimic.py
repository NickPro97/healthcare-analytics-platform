"""Load the MIMIC-III demo into the platform through the patient-service REST API.

    python -m pipeline.ingest_mimic --data ~/Desktop/patient-health-analytics/data --api http://localhost:8080

Safe to run twice: patients are matched by MRN, admissions by HADM_ID, and vitals are only
imported for patients that have none yet. History is bulk-imported (no Kafka events), so it
does not flood the alert screen; use pipeline.simulate_stream for live data.
"""
from __future__ import annotations

import argparse
import math
from pathlib import Path

import pandas as pd

from app.features import VITALS
from pipeline.api import PatientApi, default_credentials
from training import mimic_extract as mx

BATCH = 2000


def mrn_for(subject_id: int) -> str:
    return f"MIMIC-{int(subject_id)}"


def patient_body(row: pd.Series) -> dict:
    return {
        "name": f"MIMIC Demo Patient {int(row['subject_id'])}",
        "mrn": mrn_for(row["subject_id"]),
        "gender": row["gender"] if row["gender"] in ("M", "F") else "U",
        "dateOfBirth": None if pd.isna(row["dob"]) else row["dob"].date().isoformat(),
    }


def admission_body(row: pd.Series) -> dict:
    return {
        "externalId": str(int(row["hadm_id"])),
        "admitTime": row["admittime"].isoformat(),
        "dischargeTime": None if pd.isna(row["dischtime"]) else row["dischtime"].isoformat(),
        "admissionType": None if pd.isna(row["admission_type"]) else str(row["admission_type"])[:40],
        "diagnosis": None if pd.isna(row["diagnosis"]) else str(row["diagnosis"])[:255],
        "diedInHospital": bool(row["hospital_expire_flag"]),
    }


def vital_rows(vitals: pd.DataFrame) -> list[dict]:
    out = []
    for rec in vitals.itertuples(index=False):
        row = {"recordedAt": rec.charttime.isoformat()}
        for col in VITALS:
            v = getattr(rec, col)
            row[col] = None if v is None or (isinstance(v, float) and math.isnan(v)) else round(float(v), 1)
        out.append(row)
    return out


def ingest(data_dir: Path, api: PatientApi, limit: int | None = None, log=print) -> dict:
    patients = mx.load_patients(data_dir)
    admissions = mx.load_admissions(data_dir)
    vitals = mx.load_vitals(data_dir)
    if limit:
        patients = patients.head(limit)

    stats = {"patients_created": 0, "admissions_created": 0, "vitals_imported": 0}
    for _, p in patients.iterrows():
        existing = api.find_by_mrn(mrn_for(p["subject_id"]))
        if existing is None:
            existing = api.create_patient(patient_body(p))
            stats["patients_created"] += 1
        pid = existing["id"]

        known = {a.get("externalId") for a in api.admissions(pid)}
        for _, a in admissions[admissions["subject_id"] == p["subject_id"]].iterrows():
            if str(int(a["hadm_id"])) not in known:
                api.add_admission(pid, admission_body(a))
                stats["admissions_created"] += 1

        if not api.has_vitals(pid):
            rows = vital_rows(vitals[vitals["subject_id"] == p["subject_id"]])
            for i in range(0, len(rows), BATCH):
                stats["vitals_imported"] += api.import_vitals(pid, rows[i:i + BATCH])
        log(f"patient {existing['mrn']} done")
    return stats


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--data", required=True, type=Path)
    ap.add_argument("--api", default="http://localhost:8080")
    ap.add_argument("--limit", type=int, help="only the first N patients")
    args = ap.parse_args()
    api = PatientApi(args.api)
    api.login(*default_credentials())
    print(ingest(args.data.expanduser(), api, args.limit))


if __name__ == "__main__":
    main()
