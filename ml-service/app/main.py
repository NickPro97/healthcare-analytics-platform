"""FastAPI service that serves the in-hospital mortality risk model."""
from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from typing import Literal, Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from .features import aggregate_window, count_observations, rows_to_frame
from .model import LoadedModel, load_model, model_path, risk_level

log = logging.getLogger("ml-service")

state: dict[str, Optional[LoadedModel]] = {"model": None}


@asynccontextmanager
async def lifespan(_: FastAPI):
    state["model"] = load_model()
    if state["model"] is None:
        log.warning("No model at %s - train one with: python -m training.train --data <MIMIC dir>", model_path())
    yield


app = FastAPI(
    title="Healthcare Analytics - ML risk service",
    version="0.1.0",
    description="Estimates in-hospital mortality risk from the first 24h of vital signs. "
                "Demonstration model trained on the 100-patient MIMIC-III demo; NOT for clinical use.",
    lifespan=lifespan,
)


class Vital(BaseModel):
    heartRate: Optional[float] = None
    systolicBp: Optional[float] = None
    diastolicBp: Optional[float] = None
    respiratoryRate: Optional[float] = None
    spo2: Optional[float] = None
    temperatureC: Optional[float] = None


class PredictRequest(BaseModel):
    age: Optional[float] = Field(default=None, ge=0, le=130)
    gender: Optional[Literal["M", "F", "O", "U"]] = None
    vitals: list[Vital] = Field(default_factory=list, max_length=10_000)


class Factor(BaseModel):
    feature: str
    value: Optional[float]
    contribution: float


class PredictResponse(BaseModel):
    riskScore: float
    riskLevel: Literal["LOW", "MEDIUM", "HIGH"]
    modelVersion: str
    observations: int
    topFactors: list[Factor]


def require_model() -> LoadedModel:
    model = state["model"]
    if model is None:
        raise HTTPException(status_code=503, detail="Model not trained yet")
    return model


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "modelLoaded": state["model"] is not None}


@app.get("/model/info")
def model_info() -> dict:
    model = require_model()
    return {"version": model.version, **model.metadata}


@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest) -> PredictResponse:
    model = require_model()
    frame = rows_to_frame(v.model_dump() for v in req.vitals)
    row = aggregate_window(frame, req.age, req.gender)
    prob, factors = model.predict(row)
    return PredictResponse(
        riskScore=round(prob, 4),
        riskLevel=risk_level(prob),
        modelVersion=model.version,
        observations=count_observations(frame),
        topFactors=[Factor(**f) for f in factors],
    )


@app.post("/model/reload")
def reload_model() -> dict:
    state["model"] = load_model()
    return {"modelLoaded": state["model"] is not None}
