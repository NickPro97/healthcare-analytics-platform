"""Read the MIMIC-III (demo) CSV files and turn them into tidy vital-sign tables.

The raw files are NEVER copied into this repository. Point --data at your local folder.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
import pandas as pd

from app.features import MAX_AGE, VITALS, clean_vitals

# MIMIC-III item ids for each vital (CareVue + MetaVision), as used by the MIT-LCP mimic-code concepts.
ITEMIDS: dict[str, list[int]] = {
    "heartRate": [211, 220045],
    "systolicBp": [51, 442, 455, 6701, 220179, 220050],
    "diastolicBp": [8368, 8440, 8441, 8555, 220180, 220051],
    "respiratoryRate": [615, 618, 220210, 224690],
    "spo2": [646, 220277],
    "temperatureC": [223762, 676],
    "temperatureF": [223761, 678],
}
ITEM_TO_VITAL = {item: vital for vital, items in ITEMIDS.items() for item in items}
WINDOW_HOURS = 24


def _read(data_dir: Path, name: str, dates: tuple[str, ...] = ()) -> pd.DataFrame:
    """Read a MIMIC table; column names are lower-cased (the demo ships lower case, full MIMIC upper case)."""
    df = pd.read_csv(_resolve(data_dir, name))
    df.columns = [c.lower() for c in df.columns]
    for col in dates:
        df[col] = pd.to_datetime(df[col])
    return df


def load_patients(data_dir: Path) -> pd.DataFrame:
    df = _read(data_dir, "PATIENTS", dates=("dob",))
    return df[["subject_id", "gender", "dob"]]


def load_admissions(data_dir: Path) -> pd.DataFrame:
    df = _read(data_dir, "ADMISSIONS", dates=("admittime", "dischtime"))
    return df[["subject_id", "hadm_id", "admittime", "dischtime", "admission_type", "diagnosis", "hospital_expire_flag"]]


def load_vitals(data_dir: Path) -> pd.DataFrame:
    """One row per (subject, admission, charttime) with the six vitals as columns (NaN if not charted)."""
    usecols = ["subject_id", "hadm_id", "itemid", "charttime", "valuenum", "error"]
    chunks = []
    for chunk in pd.read_csv(_resolve(data_dir, "CHARTEVENTS"), usecols=lambda c: c.lower() in usecols,
                             chunksize=500_000, low_memory=False):
        chunk.columns = [c.lower() for c in chunk.columns]
        chunk = chunk[chunk["itemid"].isin(ITEM_TO_VITAL.keys())]
        chunk = chunk[(chunk["error"].isna()) | (chunk["error"] == 0)]
        chunk = chunk.dropna(subset=["valuenum", "hadm_id"])
        chunks.append(chunk[["subject_id", "hadm_id", "itemid", "charttime", "valuenum"]])
    raw = pd.concat(chunks, ignore_index=True)
    raw["vital"] = raw["itemid"].map(ITEM_TO_VITAL)
    # Fahrenheit -> Celsius
    is_f = raw["vital"] == "temperatureF"
    raw.loc[is_f, "valuenum"] = (raw.loc[is_f, "valuenum"] - 32.0) * 5.0 / 9.0
    raw.loc[is_f, "vital"] = "temperatureC"
    raw["charttime"] = pd.to_datetime(raw["charttime"])
    raw["hadm_id"] = raw["hadm_id"].astype("int64")

    wide = raw.pivot_table(index=["subject_id", "hadm_id", "charttime"], columns="vital",
                           values="valuenum", aggfunc="mean").reset_index()
    wide.columns.name = None
    wide = clean_vitals(wide)
    wide = wide.dropna(subset=VITALS, how="all")
    return wide[["subject_id", "hadm_id", "charttime", *VITALS]].sort_values(["subject_id", "charttime"])


def _resolve(data_dir: Path, name: str) -> Path:
    p = data_dir / f"{name}.csv"
    return p if p.exists() else data_dir / f"{name.lower()}.csv"


def age_years(dob: pd.Timestamp, at: pd.Timestamp) -> float:
    """Age in whole years; MIMIC shifts >89-year-olds to ~300, so cap at 90.

    Computed from calendar components because the shifted dates (years 2100-2200, and DOBs
    up to ~300 years earlier) are outside the range pandas Timedelta can subtract safely.
    """
    if pd.isna(dob) or pd.isna(at):
        return np.nan
    years = at.year - dob.year - ((at.month, at.day) < (dob.month, dob.day))
    return float(min(max(years, 0), MAX_AGE))


def first_window(vitals: pd.DataFrame) -> pd.DataFrame:
    """Observations within WINDOW_HOURS of the first charted observation in each admission."""
    start = vitals.groupby("hadm_id")["charttime"].transform("min")
    return vitals[vitals["charttime"] <= start + pd.Timedelta(hours=WINDOW_HOURS)]
