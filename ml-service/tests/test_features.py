import math

import pandas as pd

from app.features import FEATURES, aggregate_window, count_observations, rows_to_frame


def test_aggregates_ignore_missing_and_impossible_values():
    frame = rows_to_frame([
        {"heartRate": 80, "spo2": 97},
        {"heartRate": 120, "spo2": 150},       # impossible SpO2 dropped
        {"heartRate": None, "temperatureC": 38.5},
    ])
    row = aggregate_window(frame, age=67, gender="M")
    assert set(row) == set(FEATURES)
    assert row["heart_rate_mean"] == 100
    assert row["heart_rate_min"] == 80 and row["heart_rate_max"] == 120
    assert row["spo2_max"] == 97
    assert math.isnan(row["systolic_bp_mean"])
    assert row["age"] == 67 and row["male"] == 1.0


def test_age_is_capped_and_unknown_gender_is_missing():
    row = aggregate_window(rows_to_frame([]), age=300, gender="U")
    assert row["age"] == 90
    assert math.isnan(row["male"])


def test_count_observations():
    frame = rows_to_frame([{"heartRate": 80}, {"spo2": 500}, {}])
    assert count_observations(frame) == 1
    assert count_observations(pd.DataFrame()) == 0
