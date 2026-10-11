"""Writes small fake MIMIC-shaped CSV files so tests never need the real dataset."""
from __future__ import annotations

from pathlib import Path

import numpy as np
import pandas as pd


def write_fake_mimic(folder: Path, n: int = 80, seed: int = 0) -> Path:
    rng = np.random.default_rng(seed)
    folder.mkdir(parents=True, exist_ok=True)
    patients, admissions, events = [], [], []
    row = 1
    for i in range(n):
        subject, hadm = 10_000 + i, 100_000 + i
        died = int(i % 3 == 0)
        dob = pd.Timestamp("2050-01-01") + pd.Timedelta(days=int(rng.integers(0, 20_000)))
        if i == 1:
            dob = pd.Timestamp("1850-01-01")  # MIMIC-style shifted DOB for a >89-year-old
        admit = pd.Timestamp("2130-01-01") + pd.Timedelta(days=i)
        patients.append({"ROW_ID": i, "SUBJECT_ID": subject, "GENDER": "M" if i % 2 else "F",
                         "DOB": dob.strftime("%Y-%m-%d %H:%M:%S"), "EXPIRE_FLAG": died})
        admissions.append({"ROW_ID": i, "SUBJECT_ID": subject, "HADM_ID": hadm,
                           "ADMITTIME": admit.strftime("%Y-%m-%d %H:%M:%S"),
                           "DISCHTIME": (admit + pd.Timedelta(days=5)).strftime("%Y-%m-%d %H:%M:%S"),
                           "ADMISSION_TYPE": "EMERGENCY", "DIAGNOSIS": "TEST", "HOSPITAL_EXPIRE_FLAG": died})
        for h in range(0, 48, 4):  # 48h of data; only first 24h should be used
            t = (admit + pd.Timedelta(hours=1 + h)).strftime("%Y-%m-%d %H:%M:%S")
            hr = rng.normal(125 if died else 80, 8)
            for itemid, value in [(220045, hr), (220179, rng.normal(95 if died else 125, 8)),
                                  (220180, rng.normal(60, 5)), (220210, rng.normal(26 if died else 16, 2)),
                                  (220277, rng.normal(92 if died else 98, 1)), (223761, rng.normal(98.6, 0.5))]:
                events.append({"ROW_ID": row, "SUBJECT_ID": subject, "HADM_ID": hadm, "ICUSTAY_ID": 1,
                               "ITEMID": itemid, "CHARTTIME": t, "VALUE": value, "VALUENUM": value,
                               "ERROR": 0})
                row += 1
        # an implausible value and an errored row that must be ignored
        events.append({"ROW_ID": row, "SUBJECT_ID": subject, "HADM_ID": hadm, "ICUSTAY_ID": 1, "ITEMID": 220045,
                       "CHARTTIME": (admit + pd.Timedelta(hours=2)).strftime("%Y-%m-%d %H:%M:%S"),
                       "VALUE": 9999, "VALUENUM": 9999, "ERROR": 0})
        events.append({"ROW_ID": row + 1, "SUBJECT_ID": subject, "HADM_ID": hadm, "ICUSTAY_ID": 1, "ITEMID": 220045,
                       "CHARTTIME": (admit + pd.Timedelta(hours=3)).strftime("%Y-%m-%d %H:%M:%S"),
                       "VALUE": 20, "VALUENUM": 20, "ERROR": 1})
        row += 2
    pd.DataFrame(patients).to_csv(folder / "PATIENTS.csv", index=False)
    pd.DataFrame(admissions).to_csv(folder / "ADMISSIONS.csv", index=False)
    pd.DataFrame(events).to_csv(folder / "CHARTEVENTS.csv", index=False)
    return folder
