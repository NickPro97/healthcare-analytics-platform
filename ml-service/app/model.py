"""Loading the trained model and turning a feature row into a prediction + explanation."""
from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import joblib
import numpy as np
import pandas as pd

from .features import FEATURES

DEFAULT_MODEL_PATH = Path(__file__).resolve().parent.parent / "models" / "risk_model.joblib"

# Illustrative bands (see the model card): not calibrated for any specific hospital.
LOW_THRESHOLD = 0.10
HIGH_THRESHOLD = 0.30


def risk_level(p: float) -> str:
    if p >= HIGH_THRESHOLD:
        return "HIGH"
    if p >= LOW_THRESHOLD:
        return "MEDIUM"
    return "LOW"


@dataclass
class LoadedModel:
    pipeline: Any
    version: str
    metadata: dict

    def predict(self, row: dict[str, float], top_n: int = 5) -> tuple[float, list[dict]]:
        x = pd.DataFrame([row], columns=FEATURES)
        prob = float(self.pipeline.predict_proba(x)[0, 1])
        return prob, self.explain(x, top_n)

    def explain(self, x: pd.DataFrame, top_n: int) -> list[dict]:
        """Per-feature contribution to the log-odds: coefficient x standardised value.

        Only exact for a linear model on top of imputer + scaler, which is what train.py builds.
        """
        imputer = self.pipeline.named_steps["imputer"]
        scaler = self.pipeline.named_steps["scaler"]
        clf = self.pipeline.named_steps["clf"]
        z = scaler.transform(imputer.transform(x))[0]
        contributions = clf.coef_[0] * z
        order = np.argsort(-np.abs(contributions))[:top_n]
        out = []
        for i in order:
            raw = x.iloc[0, i]
            out.append({
                "feature": FEATURES[i],
                "value": None if pd.isna(raw) else round(float(raw), 2),
                "contribution": round(float(contributions[i]), 4),
            })
        return out


def model_path() -> Path:
    return Path(os.environ.get("MODEL_PATH", str(DEFAULT_MODEL_PATH)))


def load_model(path: Path | None = None) -> LoadedModel | None:
    p = path or model_path()
    if not p.exists():
        return None
    bundle = joblib.load(p)
    if bundle.get("features") != FEATURES:
        raise ValueError("Model was trained with a different feature list; retrain it.")
    return LoadedModel(pipeline=bundle["pipeline"], version=bundle["version"], metadata=bundle.get("metadata", {}))
