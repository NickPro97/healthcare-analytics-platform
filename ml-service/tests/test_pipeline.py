"""Ingest logic tested against an in-memory fake of patient-service."""
import itertools

from pipeline.ingest_mimic import ingest
from datetime import timedelta

from pipeline.simulate_stream import deteriorating_series, parse_offset


class FakeApi:
    def __init__(self):
        self.ids = itertools.count(1)
        self.patients, self.adm, self.vitals = {}, {}, {}

    def find_by_mrn(self, mrn):
        return next((p for p in self.patients.values() if p["mrn"] == mrn), None)

    def create_patient(self, body):
        assert body["gender"] in ("M", "F", "U") and body["name"] and body["mrn"].startswith("MIMIC-")
        p = {"id": next(self.ids), **body}
        self.patients[p["id"]] = p
        return p

    def admissions(self, pid):
        return self.adm.get(pid, [])

    def add_admission(self, pid, body):
        assert "T" in body["admitTime"]
        self.adm.setdefault(pid, []).append(body)
        return body

    def has_vitals(self, pid):
        return bool(self.vitals.get(pid))

    def import_vitals(self, pid, rows):
        assert len(rows) <= 2000
        for r in rows:
            assert any(r[k] is not None for k in r if k != "recordedAt")
        self.vitals.setdefault(pid, []).extend(rows)
        return len(rows)


def test_ingest_is_idempotent(fake_mimic):
    api = FakeApi()
    first = ingest(fake_mimic, api, log=lambda *_: None)
    assert first["patients_created"] == 80 and first["admissions_created"] == 80
    assert first["vitals_imported"] == 80 * 12   # 12 observation times per admission
    second = ingest(fake_mimic, api, log=lambda *_: None)
    assert second == {"patients_created": 0, "admissions_created": 0, "vitals_imported": 0}


def test_deteriorating_series_gets_worse():
    s = deteriorating_series()
    assert s[-1]["heartRate"] > s[0]["heartRate"] + 40
    assert s[-1]["systolicBp"] < s[0]["systolicBp"] - 30
    assert s[-1]["spo2"] < 93


def test_utc_offset_parsing():
    assert parse_offset("-0400") == timedelta(hours=-4)
    assert parse_offset("+05:30") == timedelta(hours=5, minutes=30)
