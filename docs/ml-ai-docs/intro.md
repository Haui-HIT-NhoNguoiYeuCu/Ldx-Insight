---
sidebar_position: 1
---

# OpenHub Data Platform

Giải pháp nguồn mở nhằm thu thập, chuẩn hóa và phân tích dữ liệu mở của các địa phương – hướng tới xây dựng chatbot phân tích chuyển đổi số.

## 1. Tổng quan

- **Crawler đa nguồn:** Đồng Tháp, TP.HCM (CKAN), Đà Nẵng (CongDuLieu), Thanh Hóa.
- **Chuẩn hóa & phân loại:** tự động gắn nhãn định dạng, chuyển CSV/XLSX thành JSON records.
- **Lên lịch dạng agent:** orchestrator điều phối từng connector, hỗ trợ cron bằng APScheduler.
- **ETL vào MongoDB:** nạp metadata, resource và bản ghi chuẩn hóa lên Mongo Atlas.
- **Hạ tầng phân tích:** dữ liệu sẵn sàng cho dashboard, API backend và mô hình machine learning.

## 2. Tính năng chính

1. **Thu thập dữ liệu**
   - Crawl qua API CKAN, REST tùy chỉnh hoặc HTML scraping.
   - Bỏ qua file đã tải, chỉ cập nhật resource mới.
2. **Chuẩn hóa**
   - Phân loại định dạng (`csv`, `xlsx`, `pdf`, `json`, …).
   - Tự chuyển CSV/XLSX thành JSON trong `storage/<connector>/normalized`.
3. **Lên lịch & log**
   - `scripts/run_agents.py [--schedule-cron "..."]`.
   - Log chi tiết vào `logs/openhub.log` và `logs/crawl_history.jsonl`.
4. **ETL MongoDB**
   - `scripts/load_to_mongo.py --mongo-uri ... --mongo-db ...`.
   - Collections: `datasets`, `resources`, `records`, `etl_runs`.
5. **Sẵn sàng cho ML/Dashboard**
   - Dataset, resource và records đã chuẩn hóa.
   - Đề xuất dùng Mongo aggregation, Superset/Metabase, FastAPI hoặc mô hình ML dự báo chỉ số.

## 3. Cấu trúc dữ liệu crawled

```text
storage/
├─ <connector>/
│ ├─ metadata/
│ │ ├─ dataset_pages/page_*.json
│ │ └─ datasets/<dataset_id>.json
│ ├─ resources/<format>/<dataset_id>/file.ext
│ └─ normalized/<dataset_id>/<resource_id>.json # nếu chuẩn hóa được
logs/
├─ openhub.log
└─ crawl_history.jsonl
```

## 4. Chuẩn bị môi trường

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## 5. Chạy crawler

```bash
python scripts/run_agents.py --connectors dong-thap,hcm,da-nang --storage-root storage
```

```bash
# hoặc đặt lịch hàng ngày 02:00
python scripts/run_agents.py --connectors hcm --storage-root storage --schedule-cron "0 2 * * *"
```

## 6. ETL lên MongoDB Atlas

```bash
python scripts/load_to_mongo.py \
  --mongo-uri "$MONGO_URI" \
  --mongo-db "$MONGO_DB" \
  --storage-root storage
```

- `datasets`: metadata cấp cao.
- `resources`: thông tin file.
- `records`: dữ liệu chuẩn hóa (1 document/row).
- `etl_runs`: log thời gian, số bản ghi.

## 7. Tech Stack

| Thành phần         | Công nghệ        | Giấy phép                 |
| ------------------ | ---------------- | ------------------------- |
| HTTP client        | httpx            | BSD-3-Clause              |
| Retry              | tenacity         | Apache 2.0                |
| Data model         | pydantic         | MIT                       |
| Scheduling         | APScheduler      | MIT                       |
| Công cụ CLI/Log    | rich             | MIT                       |
| Chuẩn hóa CSV/XLSX | pandas, openpyxl | BSD-3-Clause / MIT        |
| ETL Mongo          | pymongo, dotenv  | Apache 2.0 / BSD-3-Clause |

Tất cả đều là nguồn mở.

## 8. Các connector

- `src/openhub_crawler/portals/ckan.py` – `HoChiMinhConnector`
- `src/openhub_crawler/portals/dongthap.py` – `DongThapConnector`
- `src/openhub_crawler/portals/danang.py` – `DaNangConnector`

## 9. Kiến trúc agent

- `AgentRegistry`: đăng ký/lookup connector theo slug.
- `CrawlAgentManager`: điều phối crawl, gắn repository (`storage/<slug>`), trả về thống kê.
- `scripts/run_agents.py`: CLI + scheduler.

## 10. Roadmap

- Giải captcha/API key để tải dữ liệu thô
- Crawl incremental theo `updated_time`.
- Sinh DCAT/JSON-LD, xuất bản endpoint Linked Open Data.
- Bổ sung data quality check (Great Expectations).
- Đưa normalized data vào warehouse (Postgres/DuckDB) làm nguồn BI.
- Xây mô hình ML dự báo chỉ số chuyển đổi số.
