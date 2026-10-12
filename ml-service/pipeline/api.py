"""Minimal client for patient-service used by the ingest and simulation scripts."""
from __future__ import annotations

import os

import requests


class PatientApi:
    def __init__(self, base_url: str, session: requests.Session | None = None, timeout: float = 30):
        self.base = base_url.rstrip("/")
        self.s = session or requests.Session()
        self.timeout = timeout

    def login(self, username: str, password: str) -> None:
        r = self.s.post(f"{self.base}/api/auth/login", json={"username": username, "password": password},
                        timeout=self.timeout)
        r.raise_for_status()
        self.s.headers["Authorization"] = f"Bearer {r.json()['token']}"

    def find_by_mrn(self, mrn: str) -> dict | None:
        r = self.s.get(f"{self.base}/api/patients", params={"mrn": mrn}, timeout=self.timeout)
        r.raise_for_status()
        found = r.json()
        return found[0] if found else None

    def create_patient(self, body: dict) -> dict:
        r = self.s.post(f"{self.base}/api/patients", json=body, timeout=self.timeout)
        r.raise_for_status()
        return r.json()

    def admissions(self, patient_id: int) -> list[dict]:
        r = self.s.get(f"{self.base}/api/patients/{patient_id}/admissions", timeout=self.timeout)
        r.raise_for_status()
        return r.json()

    def add_admission(self, patient_id: int, body: dict) -> dict:
        r = self.s.post(f"{self.base}/api/patients/{patient_id}/admissions", json=body, timeout=self.timeout)
        r.raise_for_status()
        return r.json()

    def has_vitals(self, patient_id: int) -> bool:
        r = self.s.get(f"{self.base}/api/patients/{patient_id}/vitals", params={"limit": 1}, timeout=self.timeout)
        r.raise_for_status()
        return len(r.json()) > 0

    def import_vitals(self, patient_id: int, rows: list[dict]) -> int:
        r = self.s.post(f"{self.base}/api/patients/{patient_id}/vitals/batch", json=rows, timeout=self.timeout)
        r.raise_for_status()
        return r.json()["imported"]

    def record_vital(self, patient_id: int, row: dict) -> dict:
        r = self.s.post(f"{self.base}/api/patients/{patient_id}/vitals", json=row, timeout=self.timeout)
        r.raise_for_status()
        return r.json()

    def list_patients(self) -> list[dict]:
        r = self.s.get(f"{self.base}/api/patients", timeout=self.timeout)
        r.raise_for_status()
        return r.json()


def default_credentials() -> tuple[str, str]:
    """Credentials come only from the environment; there is no built-in password."""
    password = os.environ.get("HAP_PASSWORD")
    if not password:
        raise SystemExit("Set HAP_PASSWORD (the APP_ADMIN_PASSWORD from your .env) before running this script.")
    return os.environ.get("HAP_USERNAME", "nikhil"), password
