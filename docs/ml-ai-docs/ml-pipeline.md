---
sidebar_position: 6
title: Công nghệ sử dụng & Auto ML (Airflow × MLflow)
---

# Auto ML Pipeline: Airflow × MLflow

Tài liệu này mô tả kiến trúc **Auto ML** cho Ldx‑Insight: Airflow lập lịch hằng ngày để **ETL → Feature → Train → Evaluate → Register → (Optional) Batch Predict**, trong khi **MLflow** đảm nhiệm **observability/tracking** (params, metrics, artifacts) và **Model Registry** (Staging/Production).

---

## 1) Tổng quan kiến trúc

- **Airflow**: điều phối DAG hằng ngày (`@daily`), idempotent, có retry/SLA/alert.
- **Kho dữ liệu**: MongoDB / S3/MinIO (raw, features, artifacts).
- **MLflow Tracking**: log tham số/metrics/artifacts, quản lý **experiment/run**.
- **MLflow Model Registry**: lưu các _model version_ và stage **Staging/Production**.
- **Dashboard/API**: đọc dữ liệu dự báo/báo cáo từ DB sau khi batch predict.

Luồng tổng quát:  
`Data → (extract, validate) → build_features → train_pillars (CQS/KTS/XHS) → evaluate DTI → register/transition → (optional) batch_predict → dashboard`.

---

## 2) Lập lịch hằng ngày với Airflow

- DAG chạy theo lịch `@daily` (ví dụ 00:30 VN).
- Mỗi task ghi output theo **timestamp** (ví dụ `/data/features/YYYY-MM-DD/…`) để **repro** và **backfill**.
- Fail sớm ở bước **validate** (data quality gates) để tránh train trên dữ liệu xấu.

**Ví dụ DAG rút gọn (`dags/dag_train_pillars.py`)**:

```python
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta

default_args = {"owner": "ldx-insight", "retries": 1, "retry_delay": timedelta(minutes=10)}

with DAG(
    dag_id="dag_train_pillars",
    start_date=datetime(2025, 1, 1),  # chỉnh theo môi trường của bạn
    schedule="@daily",
    catchup=False,
    default_args=default_args,
    tags=["ml", "train"],
) as dag:

    def extract_raw(**_):  # tải dữ liệu raw → /data/raw/{{ ds }}/raw.parquet
        ...

    def validate_raw(**_):  # kiểm tra schema/NA/range, raise nếu fail
        ...

    def build_features(**_):  # chuẩn hoá, tạo feature → /data/features/{{ ds }}/features.parquet
        ...

    def train_and_log(**_):
        """
        Train 3 mô hình CQS/KTS/XHS, log vào MLflow:
        - params, metrics (MAE, R²), artifacts (model, plots)
        - register model (Model Registry) với tên: ldx-cqs, ldx-kts, ldx-xhs
        """
        import mlflow, mlflow.sklearn
        from sklearn.ensemble import RandomForestRegressor
        from sklearn.metrics import mean_absolute_error
        import numpy as np

        # TODO: Load X_train, y_train, X_test, y_test từ features
        mlflow.set_experiment("ldx-insight-pillars")

        for pillar in ["CQS", "KTS", "XHS"]:
            with mlflow.start_run(run_name=f"train_{pillar}") as run:
                params = dict(n_estimators=500, max_depth=4, random_state=42)
                model = RandomForestRegressor(**params).fit(X_train, y_train[pillar])
                y_pred = np.clip(model.predict(X_test), 0, 1)
                mae = float(mean_absolute_error(y_test[pillar], y_pred))

                mlflow.log_params(params)
                mlflow.log_metric("mae", mae)
                mlflow.sklearn.log_model(
                    model,
                    artifact_path="model",
                    registered_model_name=f"ldx-{pillar.lower()}"
                )
                # (tuỳ chọn) log thêm artifacts: shap plots, feature importance, csv prediction...

        # (tuỳ chọn) tính MAE cho DTI tổng hợp từ 3 trụ cột và log vào 1 run tổng hợp

    def transition_if_better(**_):
        """
        Nếu model mới tốt hơn Production (theo ngưỡng), transition stage → Production.
        """
        import mlflow
        from mlflow.tracking import MlflowClient
        client = MlflowClient()
        delta = 0.0  # hoặc 0.005 tuỳ rule

        for name in ["ldx-cqs", "ldx-kts", "ldx-xhs"]:
            prod = client.get_latest_versions(name, stages=["Production"])
            prod_mae = float("inf")
            if prod:
                run_id_prod = prod[0].run_id
                prod_mae = client.get_run(run_id_prod).data.metrics.get("mae", float("inf"))

            # TODO: Lấy version mới nhất (vừa register) từ run/train step (XCom)
            # new_version = ...
            # new_run_id = client.get_model_version(name, new_version).run_id
            # new_mae = client.get_run(new_run_id).data.metrics.get("mae", float("inf"))

            # if new_mae <= prod_mae - delta:
            #     client.transition_model_version_stage(
            #         name, new_version, stage="Production", archive_existing_versions=True
            #     )

    t1 = PythonOperator(task_id="extract_raw", python_callable=extract_raw)
    t2 = PythonOperator(task_id="validate_raw", python_callable=validate_raw)
    t3 = PythonOperator(task_id="build_features", python_callable=build_features)
    t4 = PythonOperator(task_id="train_and_log", python_callable=train_and_log)
    t5 = PythonOperator(task_id="transition_if_better", python_callable=transition_if_better)

    t1 >> t2 >> t3 >> t4 >> t5
```

> Dùng **XCom** để truyền đường dẫn/metric/version giữa các task; cấu hình **Airflow Connections** cho Mongo/S3/MLflow.

---

## 3) ML observability với MLflow

- **Experiments / Runs**: mỗi lần train log **params**, **metrics** (MAE, R², drift), **artifacts** (model, biểu đồ).
- **Model Registry**: tạo **version** cho `ldx-cqs`, `ldx-kts`, `ldx-xhs`; **transition** stage `Staging/Production` theo rule.
- **Nguồn gốc (provenance)**: log **git commit**, **dataset path**, **feature schema** → đảm bảo reproducibility.
- **Hậu triển khai**: log **inference metrics** hằng ngày/tuần, **data drift**; so sánh Production vs. candidate.

Ví dụ log tối thiểu trong step train:

```python
import mlflow, mlflow.sklearn
with mlflow.start_run(run_name="train_CQS"):
    mlflow.log_params({"n_estimators": 500, "max_depth": 4})
    mlflow.log_metric("mae", mae_cqs)
    mlflow.sklearn.log_model(model_cqs, "model", registered_model_name="ldx-cqs")
```

---

## 4) Chất lượng & an toàn khi vận hành

- **Data Quality Gates**: kiểm tra schema, NA ratio, range bound, outlier rate; fail sớm nếu vượt ngưỡng.
- **Model Quality Gates**: chỉ **transition** khi metric ≤ Production − δ hoặc ≤ ngưỡng tuyệt đối.
- **Versioning**: snapshot dữ liệu/features theo ngày; cố định seed; lưu artifact path có timestamp.
- **Rollback**: dùng Registry để **archive** version cũ và **transition lại** khi cần.
- **Thông báo**: Airflow → Slack/Email khi pipeline fail hoặc khi có bản Production mới.

---

## 5) Triển khai & Batch Predict (tuỳ chọn)

- **Batch predict** chạy sau khi transition: tạo bảng **predictions** cho năm hiện tại (hoặc tháng/quý).
- API/Dashboard đọc từ bảng này để hiển thị **leaderboard**, **phân bố high/medium/low**, v.v.

---

# Cài đặt & Vận hành ML Pipeline (nohup)

## Yêu cầu hệ thống

- Hệ điều hành Linux (hoặc macOS)
- Python 3.10+
- `pip` / `venv`
- Quyền ghi thư mục để lưu trữ logs, database, và artifacts.

---

## 6) Cài đặt Môi trường Python

Tạo môi trường ảo và cài các thư viện cần thiết.

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install --upgrade pip

# Cách nhanh (đơn giản)
pip install apache-airflow mlflow scikit-learn pandas pendulum
```

> **Gợi ý (chuẩn Airflow):** Có thể dùng file constraints tương ứng phiên bản để cài đặt Airflow ổn định hơn.

---

## 7) Cấu hình và Chạy Services

### A. Khởi chạy MLflow Server

1. **Chạy MLflow server bằng `nohup`:**

```bash
nohup mlflow server   --host 0.0.0.0   --port 5000   --allowed-hosts "localhost,34.87.86.201,34.87.86.201:5000"   > mlflow.log 2>&1 &
```

2. **Kiểm tra:**

- Giao diện MLflow: `http://<dia_chi_ip_may_chu>:5000`
- Xem log: `tail -f mlflow.log`
- Tìm PID để dừng: `ps aux | grep "mlflow server"`

> **Tuỳ chọn:** cấu hình `--backend-store-uri` (MySQL/Postgres) và `--default-artifact-root` (S3/MinIO).

### B. Khởi chạy Airflow

1. **Cấu hình thư mục DAGs:** mặc định `~/airflow/dags` (hoặc chỉnh `AIRFLOW__CORE__DAGS_FOLDER`).  
   Viết file DAG và lưu vào thư mục trên.

2. **Chạy Airflow bằng `nohup`:**

```bash
# Khởi tạo DB & user lần đầu
airflow db init
airflow users create --username admin --firstname Admin --lastname User --role Admin --email admin@example.com --password admin

# Webserver & Scheduler
nohup airflow webserver --port 8080 > airflow_webserver.log 2>&1 &
nohup airflow scheduler > airflow_scheduler.log 2>&1 &
```

3. **Kiểm tra:**

- Giao diện Airflow: `http://<dia_chi_ip_may_chu>:8080`
- Log: `tail -f airflow_webserver.log` / `tail -f airflow_scheduler.log`
- PIDs: `ps aux | grep "airflow webserver"` / `ps aux | grep "airflow scheduler"`

---

## 8) Vận hành Pipeline

1. Mở Airflow UI: `http://<ip_may_chu>:8080`.
2. **Un-pause** DAG để bật lịch.
3. Theo dõi từng task; có thể **Trigger** thủ công.
4. Kiểm tra MLflow UI: `http://<ip_may_chu>:5000` để xem **Experiment/Run**, metrics & artifacts.

---

## 9) Checklist triển khai nhanh

- [ ] Tạo **DAG**: `dag_build_features`, `dag_train_pillars`, `dag_predict_batch`
- [ ] Kết nối MLflow (`MLFLOW_TRACKING_URI`), xác thực nếu có
- [ ] Log params/metrics/artifacts, **register model** mỗi run
- [ ] So sánh với Production → **transition stage** nếu đạt
- [ ] Batch predict → cập nhật DB cho dashboard
- [ ] Alert/monitoring: email/Slack + health checks

---

## 10) Ghi chú cho Ldx‑Insight

- 3 trụ cột **CQS/KTS/XHS** huấn luyện bằng **RandomForestRegressor**; `DTI = 0.35*CQS + 0.35*KTS + 0.30*XHS`.
- Đảm bảo **schema features** ổn định để thay thế dữ liệu giả bằng ETL thật **không đổi cấu trúc**.
- Khi đủ dữ liệu thời gian (quarterly/monthly), cân nhắc **TimeSeriesSplit** và drift monitoring.

---

## 11) Tài liệu tham khảo

- Airflow docs: https://airflow.apache.org/
- MLflow docs: https://mlflow.org/docs/latest/index.html
- Model Registry: https://mlflow.org/docs/latest/model-registry.html
- Scikit-learn metrics: https://scikit-learn.org/stable/modules/model_evaluation.html
