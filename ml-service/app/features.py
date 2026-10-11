"""Feature engineering shared by training and serving.

Training and the live API call the SAME functions, so the model always sees features
computed the same way (no training/serving skew).
"""
from __future__ import annotations

from typing import Iterable, Mapping

import numpy as np
import pandas as pd

# Column names match the patient-service JSON fields.
VITALS = ["heartRate", "systolicBp", "diastolicBp", "respiratoryRate", "spo2", "temperatureC"]

# Snake-case names used in feature names and the UI.
VITAL_LABELS = {
    "heartRate": "heart_rate",
    "systolicBp": "systolic_bp",
    "diastolicBp": "diastolic_bp",
    "respiratoryRate": "respiratory_rate",
    "spo2": "spo2",
    "temperatureC": "temperature_c",
}

# Physiologically possible ranges (inclusive). Values outside are treated as charting errors.
# Kept identical to the validation limits on the Java VitalSign entity.
VALID_RANGES = {
    "heartRate": (1, 300),
    "systolicBp": (1, 300),
    "diastolicBp": (1, 200),
    "respiratoryRate": (1, 80),
    "spo2": (1, 100),
    "temperatureC": (25, 45),
}

AGGREGATES = ["mean", "min", "max"]
MAX_AGE = 90.0

FEATURES: list[str] = [f"{VITAL_LABELS[v]}_{agg}" for v in VITALS for agg in AGGREGATES] + ["age", "male"]


def clean_vitals(df: pd.DataFrame) -> pd.DataFrame:
    """Return a copy with out-of-range values set to NaN."""
    out = df.copy()
    for col in VITALS:
        if col not in out.columns:
            out[col] = np.nan
        out[col] = pd.to_numeric(out[col], errors="coerce")
        lo, hi = VALID_RANGES[col]
        out.loc[(out[col] < lo) | (out[col] > hi), col] = np.nan
    return out


def aggregate_window(vitals: pd.DataFrame, age: float | None, gender: str | None) -> dict[str, float]:
    """Summarise one 24-hour window of observations into a single feature row."""
    v = clean_vitals(vitals)
    row: dict[str, float] = {}
    for col in VITALS:
        series = v[col].dropna()
        label = VITAL_LABELS[col]
        if series.empty:
            for agg in AGGREGATES:
                row[f"{label}_{agg}"] = np.nan
        else:
            row[f"{label}_mean"] = float(series.mean())
            row[f"{label}_min"] = float(series.min())
            row[f"{label}_max"] = float(series.max())
    row["age"] = np.nan if age is None else float(min(max(age, 0.0), MAX_AGE))
    row["male"] = np.nan if gender not in ("M", "F") else (1.0 if gender == "M" else 0.0)
    return row


def rows_to_frame(vitals: Iterable[Mapping[str, float | None]]) -> pd.DataFrame:
    """Turn a list of vital-sign dicts (API payload) into a DataFrame with all vital columns."""
    df = pd.DataFrame(list(vitals))
    for col in VITALS:
        if col not in df.columns:
            df[col] = np.nan
    return df[VITALS]


def count_observations(vitals: pd.DataFrame) -> int:
    """Number of observation sets with at least one valid measurement."""
    if vitals.empty:
        return 0
    return int(clean_vitals(vitals)[VITALS].notna().any(axis=1).sum())
