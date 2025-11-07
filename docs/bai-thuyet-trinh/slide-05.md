---
sidebar_position: 5
title: Slide 5 - Tích hợp & Chuẩn hóa dữ liệu
---

# Tiêu chí 1: Tích hợp & Chuẩn hóa dữ liệu

## Nguồn dữ liệu đã tích hợp

- **Nguồn 1 – Đồng Tháp Open Data** (`https://opendata.dongthap.gov.vn`) – API JSON tùy chỉnh.
- **Nguồn 2 – TP. Hồ Chí Minh Open Data** (`https://data.hochiminhcity.gov.vn`) – Nền tảng CKAN, dữ liệu JSON/CSV.
- **Nguồn 3 – Đà Nẵng Open Data** (`https://opendata.danang.gov.vn`) – CKAN, hỗ trợ API REST.
- (Roadmap) **Nguồn 4 – Thanh Hóa Open Data** – đang tích hợp thử nghiệm.

## Quy trình ETL tự động

1. **Extract**: Scheduler gọi lần lượt các connector, tải metadata + file tài nguyên.
2. **Transform**:
   - Chuẩn hóa schema về JSON thống nhất bằng `pydantic`.
   - Phân loại và chuyển đổi CSV/XLSX → JSON line bằng `pandas`.
   - Ghi log lỗi, retry với `tenacity` khi cổng dữ liệu không ổn định.
3. **Load**: Lưu vào MongoDB Atlas (collections `datasets`, `categories`, `download_logs`) và đồng bộ file vào kho lưu trữ.

> Kết quả: 1.200+ bản ghi dataset đã được chuẩn hóa, đảm bảo toàn vẹn và truy xuất nhất quán.
