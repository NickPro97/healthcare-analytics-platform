import pytest

from tests.synthetic import write_fake_mimic
from training.train import train


@pytest.fixture(scope="session")
def fake_mimic(tmp_path_factory):
    return write_fake_mimic(tmp_path_factory.mktemp("mimic"))


@pytest.fixture(scope="session")
def trained_model_path(fake_mimic, tmp_path_factory):
    out = tmp_path_factory.mktemp("model") / "risk_model.joblib"
    train(fake_mimic, out)
    return out


@pytest.fixture()
def client(trained_model_path, monkeypatch):
    monkeypatch.setenv("MODEL_PATH", str(trained_model_path))
    from fastapi.testclient import TestClient
    from app.main import app
    with TestClient(app) as c:
        yield c
