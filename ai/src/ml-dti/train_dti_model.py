"""Train RandomForest model to predict CQS/KTS/XHS pillars and aggregate DTI."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Dict

import numpy as np
import pandas as pd
from joblib import dump
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error, r2_score


DATA_PATH = Path(__file__).with_name("synthetic_dti_train.csv")
MODEL_DIR = Path(__file__).with_name("artifacts")

FEATURE_COLS = [
    "datasets_total",
    "updated_12m_ratio",
    "open_format_ratio",
    "has_api_ratio",
    "egov_datasets_share",
    "econ_datasets_share",
    "social_datasets_share",
    "online_public_services_ratio",
    "online_applications_ratio",
]

PILLAR_COLUMNS = ["CQS", "KTS", "XHS"]
PILLAR_WEIGHTS = np.array([0.4, 0.35, 0.25])


def load_dataset() -> pd.DataFrame:
    return pd.read_csv(DATA_PATH)


def train_model(df: pd.DataFrame) -> None:
    train_mask = df["year"] < 2024
    test_mask = df["year"] == 2024

    X_train = df.loc[train_mask, FEATURE_COLS]
    y_train = df.loc[train_mask, PILLAR_COLUMNS]

    X_test = df.loc[test_mask, FEATURE_COLS]
    y_true_dti = df.loc[test_mask, "DTI_total"].values

    model = RandomForestRegressor(
        n_estimators=500,
        max_depth=4,
        random_state=42,
    )
    model.fit(X_train, y_train)

    pillar_pred = np.clip(model.predict(X_test), 0.0, 1.0)
    dti_pred = pillar_pred @ PILLAR_WEIGHTS

    mae = mean_absolute_error(y_true_dti, dti_pred)
    r2 = r2_score(y_true_dti, dti_pred)

    comparison = pd.DataFrame(
        {
            "province": df.loc[test_mask, "province"].values,
            "DTI_true_2024": y_true_dti,
            "DTI_pred_2024": dti_pred,
            "CQS_pred": pillar_pred[:, 0],
            "KTS_pred": pillar_pred[:, 1],
            "XHS_pred": pillar_pred[:, 2],
            "abs_error": np.abs(y_true_dti - dti_pred),
        }
    ).sort_values("province")

    print("=== Evaluation on 2024 (hold-out) ===")
    print(f"MAE: {mae:.4f}")
    print(f"R2 : {r2:.4f}")
    print("\n=== 2024 Prediction vs Ground Truth ===")
    print(
        comparison[
            ["province", "DTI_true_2024", "DTI_pred_2024", "CQS_pred", "KTS_pred", "XHS_pred", "abs_error"]
        ].to_string(index=False, formatters={"DTI_pred_2024": "{:.4f}".format})
    )

    save_artifacts(model, comparison, {"mae": mae, "r2": r2})


def save_artifacts(model: RandomForestRegressor, comparison: pd.DataFrame, metrics: Dict[str, float]) -> None:
    MODEL_DIR.mkdir(exist_ok=True)
    model_path = MODEL_DIR / "random_forest_dti.joblib"
    dump(model, model_path)

    metadata = {
        "model_path": model_path.name,
        "feature_columns": FEATURE_COLS,
        "pillar_names": PILLAR_COLUMNS,
        "pillar_weights": PILLAR_WEIGHTS.tolist(),
        "train_years": [2022, 2023],
        "test_year": 2024,
        "metrics": {k: round(v, 6) for k, v in metrics.items()},
        "comparison_2024": comparison.to_dict(orient="records"),
        "prediction_bounds": [0.0, 1.0],
        "usage": {
            "forecasting": "Dự báo CQS/KTS/XHS từ feature và suy ra DTI_total = dot(weights, pillars).",
            "scenario_simulation": "Điều chỉnh feature để đánh giá tác động lên từng trụ cột và DTI tổng.",
            "api_integration": "Artefact dùng cho REST API / dashboard DX-Predictor.",
        },
    }
    metadata_path = MODEL_DIR / "metadata.json"
    metadata_path.write_text(json.dumps(metadata, indent=2), encoding="utf-8")

    print(f"\nArtifacts saved to {MODEL_DIR} (model + metadata).")


def main() -> None:
    df = load_dataset()
    train_model(df)


if __name__ == "__main__":
    main()
