def test_health_reports_model(client):
    assert client.get("/health").json() == {"status": "ok", "modelLoaded": True}


def test_model_info(client):
    info = client.get("/model/info").json()
    assert info["version"].startswith("logreg-")
    assert "metrics" in info


def test_sick_patient_scores_higher_than_stable(client):
    sick = {"age": 70, "gender": "M", "vitals": [
        {"heartRate": 128, "systolicBp": 92, "respiratoryRate": 27, "spo2": 91, "temperatureC": 38.9}] * 6}
    stable = {"age": 70, "gender": "M", "vitals": [
        {"heartRate": 78, "systolicBp": 126, "respiratoryRate": 15, "spo2": 98, "temperatureC": 36.9}] * 6}
    r_sick = client.post("/predict", json=sick).json()
    r_stable = client.post("/predict", json=stable).json()
    assert r_sick["riskScore"] > r_stable["riskScore"]
    assert r_sick["riskLevel"] == "HIGH"
    assert r_sick["observations"] == 6
    assert len(r_sick["topFactors"]) == 5
    assert {"feature", "value", "contribution"} <= set(r_sick["topFactors"][0])


def test_empty_vitals_still_answers(client):
    r = client.post("/predict", json={"vitals": []})
    assert r.status_code == 200
    assert r.json()["observations"] == 0


def test_validation(client):
    assert client.post("/predict", json={"age": -5}).status_code == 422
    assert client.post("/predict", json={"gender": "X"}).status_code == 422


def test_503_without_model(monkeypatch, tmp_path):
    monkeypatch.setenv("MODEL_PATH", str(tmp_path / "missing.joblib"))
    from fastapi.testclient import TestClient
    from app.main import app
    with TestClient(app) as c:
        assert c.get("/health").json()["modelLoaded"] is False
        assert c.post("/predict", json={"vitals": []}).status_code == 503
