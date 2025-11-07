"""FastAPI service exposing DTI forecast & simulation endpoints."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Dict, Optional

import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException
from joblib import load
from pydantic import BaseModel, Field, validator


ROOT = Path(__file__).resolve().parent
ARTIFACT_DIR = ROOT / "artifacts"
MODEL_PATH = ARTIFACT_DIR / "random_forest_dti.joblib"
METADATA_PATH = ARTIFACT_DIR / "metadata.json"
DATA_PATH = ROOT / "synthetic_dti_train.csv"

MODEL = load(MODEL_PATH)
METADATA = json.loads(METADATA_PATH.read_text(encoding="utf-8"))
FEATURE_COLUMNS = METADATA["feature_columns"]
PILLAR_NAMES = METADATA["pillar_names"]
PILLAR_WEIGHTS = np.array(METADATA["pillar_weights"])
HISTORICAL_DF = pd.read_csv(DATA_PATH)

app = FastAPI(
    title="Ldx-Insight DTI Forecast API",
    description=(
        "Serve the RandomForest DTI model for (1) yearly forecast "
        "(2) scenario simulation (3) dashboard integration."
    ),
    version="1.0.0",
)


class FeatureVector(BaseModel):
    """Explicit schema for model input features."""

    datasets_total: float = Field(..., ge=0, le=1)
    updated_12m_ratio: float = Field(..., ge=0, le=1)
    open_format_ratio: float = Field(..., ge=0, le=1)
    has_api_ratio: float = Field(..., ge=0, le=1)
    egov_datasets_share: float = Field(..., ge=0, le=1)
    econ_datasets_share: float = Field(..., ge=0, le=1)
    social_datasets_share: float = Field(..., ge=0, le=1)
    online_public_services_ratio: float = Field(..., ge=0, le=1)
    online_applications_ratio: float = Field(..., ge=0, le=1)

    def to_vector(self) -> Dict[str, float]:
        return self.model_dump()


class ForecastRequest(BaseModel):
    province: str
    year: int
    features: Optional[FeatureVector] = None


class SimulationRequest(BaseModel):
    province: str
    year: int
    deltas: Dict[str, float] = Field(
        ...,
        description="Relative adjustments to baseline feature values. Positive => improvement.",
    )
    base_features: Optional[FeatureVector] = None

    @validator("deltas")
    def validate_feature_keys(cls, value: Dict[str, float]) -> Dict[str, float]:
        unknown = set(value) - set(FEATURE_COLUMNS)
        if unknown:
            raise ValueError(f"Unknown feature(s): {unknown}")
        return value


def load_baseline_features(province: str, year: int) -> FeatureVector:
    row = HISTORICAL_DF.loc[
        (HISTORICAL_DF["province"].str.lower() == province.lower())
        & (HISTORICAL_DF["year"] == year)
    ]
    if row.empty:
        raise HTTPException(
            status_code=404,
            detail=f"No baseline features for province={province}, year={year}",
        )
    features = row.iloc[0][FEATURE_COLUMNS].to_dict()
    return FeatureVector(**features)


def clip_prediction(pred: float) -> float:
    return float(np.clip(pred, 0.0, 1.0))


def predict_pillars(feature_map: Dict[str, float]) -> Dict[str, float]:
    vector = [feature_map[col] for col in FEATURE_COLUMNS]
    preds = np.clip(MODEL.predict([vector])[0], 0.0, 1.0)
    return {pillar: float(score) for pillar, score in zip(PILLAR_NAMES, preds)}


def aggregate_dti(pillar_scores: Dict[str, float]) -> float:
    vec = np.array([pillar_scores[name] for name in PILLAR_NAMES])
    return clip_prediction(vec @ PILLAR_WEIGHTS)


@app.get("/metadata")
def get_metadata() -> Dict:
    return METADATA


@app.post("/forecast")
def forecast(req: ForecastRequest) -> Dict:
    features = req.features or load_baseline_features(req.province, req.year)
    feature_dict = features.to_vector()
    pillars = predict_pillars(feature_dict)
    dti_score = aggregate_dti(pillars)
    return {
        "province": req.province,
        "year": req.year,
        "pillar_scores": pillars,
        "DTI_prediction": dti_score,
        "features_used": feature_dict,
    }


@app.post("/simulate")
def simulate(req: SimulationRequest) -> Dict:
    base = req.base_features or load_baseline_features(req.province, req.year)
    feature_dict = base.to_vector()

    for key, delta in req.deltas.items():
        feature_dict[key] = float(np.clip(feature_dict[key] + delta, 0.0, 1.0))

    pillars = predict_pillars(feature_dict)
    dti_score = aggregate_dti(pillars)
    return {
        "province": req.province,
        "year": req.year,
        "scenario_deltas": req.deltas,
        "pillar_scores": pillars,
        "DTI_prediction": dti_score,
        "features_used": feature_dict,
    }


@app.get("/")
def root() -> Dict[str, str]:
    return {
        "message": "DTI model API ready. Use /forecast, /simulate, /metadata endpoints.",
    }
