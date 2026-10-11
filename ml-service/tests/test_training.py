import json

import pandas as pd

from training import mimic_extract as mx
from training.train import build_dataset


def test_extract_converts_fahrenheit_and_drops_bad_rows(fake_mimic):
    vitals = mx.load_vitals(fake_mimic)
    assert vitals["temperatureC"].between(35, 40).all()          # 98.6F -> ~37C
    assert vitals["heartRate"].max() < 300                         # 9999 removed
    assert (vitals["heartRate"] != 20).all()                       # ERROR=1 row removed


def test_first_window_keeps_only_24_hours(fake_mimic):
    window = mx.first_window(mx.load_vitals(fake_mimic))
    span = window.groupby("hadm_id")["charttime"].agg(lambda s: s.max() - s.min())
    assert (span <= pd.Timedelta(hours=24)).all()


def test_age_handles_mimic_shifted_dates():
    assert mx.age_years(pd.Timestamp("1850-01-01"), pd.Timestamp("2130-01-01")) == 90
    assert mx.age_years(pd.Timestamp("2060-06-15"), pd.Timestamp("2130-06-14")) == 69


def test_dataset_and_model_card(fake_mimic, trained_model_path):
    X, y = build_dataset(fake_mimic)
    assert len(X) == 80 and y.sum() == 27
    card = json.loads((trained_model_path.parent / "model_card.json").read_text())
    # synthetic deaths are clearly separable, so the pipeline must learn them
    assert card["metrics"]["logistic_regression_auc"] > 0.9
