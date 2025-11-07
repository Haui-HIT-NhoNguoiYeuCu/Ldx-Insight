---
sidebar_position: 3
title: Công nghệ sử dụng (ML/AI & Crawler)
---

# Công nghệ sử dụng

## Python Runtime

- **Python 3.10+**: môi trường chạy chính cho crawler & pipeline ML.

## Thư viện chính

- **httpx**: HTTP client hiện đại, hỗ trợ timeout, streaming, header.
- **tenacity**: Retry với exponential backoff cho call mạng không ổn định.
- **pydantic v2**: Mô hình dữ liệu, validate, (de)serialization.
- **pandas**: Đọc CSV/XLS/XLSX, chuyển `DataFrame` → JSON records.
- **APScheduler**: Lập lịch job (cron) chạy crawler định kỳ.
- **rich**: Log hiển thị đẹp trên CLI.

## Kiến trúc mã nguồn

- `scripts/run_agents.py`: parse args, schedule cron, ghi `crawl_history.jsonl`.
- `openhub_crawler/agents.py`: AgentResult, AgentRegistry, CrawlAgentManager.
- `openhub_crawler/portals/*`: base protocol và connectors cụ thể.
- `openhub_crawler/transform.py`: chuẩn hóa dữ liệu sang JSON.
- `openhub_crawler/storage.py`: phân loại định dạng, lưu trữ file/metadata.

## ML gợi ý (roadmap)

- **scikit-learn** (RandomForest/XGBoost): mô hình tabular.
- **statsmodels / pmdarima**: chuỗi thời gian (ARIMA, SARIMA).
- **Prophet**: dự báo xu hướng theo mùa vụ.
- **lightgbm/catboost**: tăng cường cho dữ liệu nhiều đặc trưng.

## DevOps & Triển khai

- Chạy độc lập bằng virtualenv/conda.
- Airflow để lập lịch hàng ngày, xử lý dữ liệu và huấn luyện mô hình
