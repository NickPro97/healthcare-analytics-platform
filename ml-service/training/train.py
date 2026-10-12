"""Train the in-hospital mortality model on the MIMIC-III demo.

Usage (from ml-service/):
    python -m training.train --data ~/Desktop/patient-health-analytics/data

Writes models/risk_model.joblib (git-ignored) and models/model_card.json.
"""
from __future__ import annotations

import argparse
import json
from datetime import date
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import roc_auc_score
from sklearn.model_selection import StratifiedKFold, cross_val_predict
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

from app.features import FEATURES, aggregate_window
from app.model import DEFAULT_MODEL_PATH
from training import mimic_extract as mx

SEED = 42


def build_dataset(data_dir: Path) -> tuple[pd.DataFrame, pd.Series]:
    patients = mx.load_patients(data_dir).set_index("subject_id")
    admissions = mx.load_admissions(data_dir).set_index("hadm_id")
    window = mx.first_window(mx.load_vitals(data_dir))

    rows, labels, ids = [], [], []
    for hadm_id, group in window.groupby("hadm_id"):
        if hadm_id not in admissions.index:
            continue
        adm = admissions.loc[hadm_id]
        pat = patients.loc[adm["subject_id"]]
        age = mx.age_years(pat["dob"], adm["admittime"])
        rows.append(aggregate_window(group, age, pat["gender"]))
        labels.append(int(adm["hospital_expire_flag"]))
        ids.append(hadm_id)
    X = pd.DataFrame(rows, columns=FEATURES, index=pd.Index(ids, name="hadm_id"))
    y = pd.Series(labels, index=X.index, name="died_in_hospital")
    return X, y


def make_pipeline() -> Pipeline:
    return Pipeline([
        ("imputer", SimpleImputer(strategy="median")),
        ("scaler", StandardScaler()),
        # Strong-ish L2 regularisation: ~130 samples vs 20 features.
        ("clf", LogisticRegression(C=0.3, max_iter=2000)),
    ])


def repeated_cv_auc(make, X: pd.DataFrame, y: pd.Series, repeats: int = 10) -> tuple[float, float]:
    """Mean and standard deviation of out-of-fold ROC AUC over repeated stratified 5-fold CV.

    With ~125 admissions a single CV split is noisy, so we repeat it with different shuffles.
    """
    aucs = []
    for r in range(repeats):
        cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=SEED + r)
        p = cross_val_predict(make(), X, y, cv=cv, method="predict_proba")[:, 1]
        aucs.append(roc_auc_score(y, p))
    return float(np.mean(aucs)), float(np.std(aucs))


def evaluate(X: pd.DataFrame, y: pd.Series) -> dict:
    def gb():
        return Pipeline([("imputer", SimpleImputer(strategy="median")),
                         ("clf", GradientBoostingClassifier(random_state=SEED, n_estimators=100, max_depth=2))])
    lr_mean, lr_sd = repeated_cv_auc(make_pipeline, X, y)
    gb_mean, gb_sd = repeated_cv_auc(gb, X, y)
    return {
        "cv": "10 x stratified 5-fold, out-of-fold ROC AUC (mean +/- sd)",
        "logistic_regression_auc": round(lr_mean, 3),
        "logistic_regression_auc_sd": round(lr_sd, 3),
        "gradient_boosting_auc": round(gb_mean, 3),
        "gradient_boosting_auc_sd": round(gb_sd, 3),
        "interpretation": "Close to chance (0.5): 125 admissions and vitals alone are not enough to predict "
                          "mortality well. The pipeline is the deliverable; retrain on a larger cohort for a useful model.",
    }


def train(data_dir: Path, out: Path) -> dict:
    X, y = build_dataset(data_dir)
    metrics = evaluate(X, y)
    pipeline = make_pipeline().fit(X, y)
    version = f"logreg-{date.today():%Y%m%d}"
    metadata = {
        "task": "in-hospital mortality from first 24h of charted vitals",
        "dataset": "MIMIC-III Clinical Database Demo v1.4 (PhysioNet, ODbL)",
        "admissions": int(len(X)),
        "deaths": int(y.sum()),
        "features": FEATURES,
        "metrics": metrics,
        "trained_on": date.today().isoformat(),
        "intended_use": "Engineering demonstration only. Not validated. Not for clinical decisions.",
    }
    out.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump({"pipeline": pipeline, "features": FEATURES, "version": version, "metadata": metadata}, out)
    (out.parent / "model_card.json").write_text(json.dumps({"version": version, **metadata}, indent=2))
    return {"version": version, **metadata}


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--data", required=True, type=Path, help="folder with MIMIC-III demo CSV files")
    ap.add_argument("--out", type=Path, default=DEFAULT_MODEL_PATH)
    args = ap.parse_args()
    card = train(args.data.expanduser(), args.out)
    print(json.dumps({k: card[k] for k in ("version", "admissions", "deaths", "metrics")}, indent=2))


if __name__ == "__main__":
    main()
