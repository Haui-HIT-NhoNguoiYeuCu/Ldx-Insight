# Bài toán mô hình & công thức huấn luyện

Tài liệu này mô tả kiến trúc ML của Ldx‑Insight để dự báo 3 trụ cột **Chính quyền số (CQS)**, **Kinh tế số (KTS)**, **Xã hội số (XHS)** và chỉ số tổng hợp **DTI**.

$$DTI = 0.35 \cdot CQS + 0.35 \cdot KTS + 0.30 \cdot XHS$$

Pipeline bao trùm từ Data → Airflow → MongoDB → API → ML → Dashboard.

## 1. Đặc tả bài toán

- **Bản chất**: **multi-output regression** trên dữ liệu bảng `province × year`
- **Đầu vào**: 22 feature thu thập từ dữ liệu mở, thống kê hành chính, TMĐT, kỹ năng số (xem Phụ lục)
- **Đầu ra**: `CQS, KTS, XHS ∈ [0,1]`. DTI được tính theo công thức cố định sau khi dự báo 3 trụ cột

## 2. Tiền xử lý

- `generate_features.py` (Airflow `dag_build_features`) sinh dữ liệu giả (fake) cho 4 tỉnh (Đà Nẵng, TP.HCM, Thanh Hóa, Đồng Tháp) giai đoạn 2022‑2024. Các trường tỷ lệ (ratio) được clamp về [0,1], các trường đếm > 0
- Khi có dữ liệu thật chỉ cần thay module generate bằng crawler/ETL; schema giữ nguyên
- Trước khi huấn luyện (train): chuẩn hóa các trường đếm bằng cách chia cho max, xử lý thiếu bằng median imputation

## 3. Mô hình

| Trụ cột | Model |
|---------|-------|
| CQS | RandomForestRegressor |
| KTS | RandomForestRegressor |
| XHS | RandomForestRegressor |

## 4. Hàm mất mát & đánh giá

**MAE (Mean Absolute Error):**
$$\text{MAE} = \frac{1}{N} \sum | \hat{y} - y |$$

**R² (R-squared):**
$$R^2 = 1 - \frac{\sum (y - \hat{y})^2}{\sum (y - \bar{y})^2}$$

- Đánh giá từng trụ cột trên tập test (năm 2024)
- DTI suy ra từ 3 trụ cột: `DTI_pred = 0.35*CQS_pred + 0.35*KTS_pred + 0.30*XHS_pred`, báo cáo thêm MAE

## 5. Pseudo-code huấn luyện 3 trụ cột

```python
df = fetch_features()
df = compute_targets(df)

X_train = df[df.year < 2024][FEATURE_COLS]
X_test  = df[df.year == 2024][FEATURE_COLS]

models = {}
for pillar in ["CQS", "KTS", "XHS"]:
  y_train = df[df.year < 2024][pillar]
  y_test  = df[df.year == 2024][pillar]
  
  model = RandomForestRegressor(n_estimators=500, max_depth=4, random_state=42)
  model.fit(X_train, y_train)
  
  y_pred = np.clip(model.predict(X_test), 0, 1)
  mae = mean_absolute_error(y_test, y_pred)
  print(pillar, "MAE=", mae)
  
  save_model(model, f"rf_{pillar.lower()}", {"mae": mae}, [2022, 2023])
  models[pillar] = model

# Đánh giá DTI tổng hợp
cqs = models["CQS"].predict(X_test)
kts = models["KTS"].predict(X_test)
xhs = models["XHS"].predict(X_test)
dti_pred = 0.35*cqs + 0.35*kts + 0.30*xhs

print("DTI MAE=", mean_absolute_error(df[df.year == 2024]["DTI_total"], dti_pred))
```

## 6. API phục vụ

- GET `/api/features`, `/api/features/all`: Airflow/ML đọc dữ liệu train từ Mongo
- POST `/api/predictions/batch {"year": 2025}`: Server load model mới nhất, dự báo batch, ghi vào collection predictions
- POST `/api/simulate {province, year, adjustments}`: Override feature, trả về `{cqs_pred, kts_pred, xhs_pred, dti_pred}`
- GET `/api/dx/dashboard`: Trả dữ liệu leaderboard + phân bố high/medium/low cho DX-Predictor

## 7. Airflow DAGs

- `dag_build_features`: Tạo CSV/JSON + upsert vào Mongo
- `dag_train_pillars`: Huấn luyện lại mô hình, cập nhật registry (Mongo hoặc file local)
- `dag_predict_batch`: Dự báo định kỳ cho năm hiện tại, cấp dữ liệu cho dashboard

## 8. Khuyến nghị dữ liệu

- Cần ít nhất 3 năm dữ liệu/tỉnh, tối thiểu 80% giá trị hợp lệ trên mỗi feature
- Khi chuyển sang dữ liệu theo quý/tháng → dùng TimeSeriesSplit

## 9. Lộ trình nâng cao

- Thử nghiệm LightGBM/CatBoost, Multitask learning
- Tích hợp SHAP explainability  
- Near real-time streaming với Kafka/Airflow

## Phụ lục – Feature theo nhóm trụ cột

**CQS**: datasets_total, open_format_ratio, has_api_ratio, online_public_services_ratio, metadata_completeness_ratio, open_license_ratio, egov_datasets_share, updated_12m_ratio

**KTS**: biz_open_data_ratio, ecommerce_index_proxy, open_finance_datasets_ratio, e_payment_usage_ratio, innovation_datasets_share, data_reuse_ratio, private_sector_datasets

**XHS**: digital_literacy_rate, education_open_data_ratio, health_open_data_ratio, telehealth_usage_ratio, citizen_feedback_ratio, internet_access_ratio, mobile_penetration_ratio
