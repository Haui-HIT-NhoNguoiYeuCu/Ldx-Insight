---
sidebar_position: 2
title: Chức Năng Chính
---

Hệ thống **Ldx-Insight** được xây dựng để đáp ứng 4 nhóm chức năng bắt buộc của cuộc thi. Các chức năng này được chia thành các module (Backend, Frontend, ML Service, Data Collector) để đảm bảo tính rõ ràng và hiệu quả.

---

### 1. Tích hợp & Chuẩn hóa Dữ liệu

Đây là chức năng lõi của module **Data Collector (Python)**.

- **Thu thập đa nguồn:** Tự động gọi API hoặc tải dữ liệu từ các nguồn dữ liệu mở được chỉ định (tối thiểu 3 nguồn, ví dụ: data.gov.vn, opendata.mic.gov.vn,...).
- **Chuẩn hóa JSON:** Xử lý và chuyển đổi dữ liệu thô (có thể là CSV, XML,...) về một định dạng JSON thống nhất.
- **Lưu trữ tập trung:** Nạp (load) dữ liệu JSON đã được chuẩn hóa vào cơ sở dữ liệu trung tâm (MongoDB) để Backend có thể truy vấn.
- **Ghi nhận lỗi:** Ghi lại log về thời gian tải, nguồn dữ liệu và các lỗi kết nối (nếu có) trong quá trình thu thập.

### 2. Hiển thị & Phân tích Dữ liệu 

Đây là chức năng chính của **Frontend Dashboard (Nuxt.js)**, sử dụng dữ liệu từ Backend API.

-**Trực quan hóa Dữ liệu:** Cung cấp các biểu đồ, bảng dữ liệu, và bản đồ (nếu có dữ liệu địa lý) để trực quan hóa các chỉ số.
-**Tìm kiếm & Lọc:** Cho phép người dùng (người dân, doanh nghiệp) tìm kiếm các bộ dữ liệu, hoặc lọc, thống kê dữ liệu theo các tiêu chí (ví dụ: tỉnh, lĩnh vực, năm).
- **Thống kê Hub:** Cung cấp các thống kê về chính nền tảng (theo ý tưởng của nhóm) như tổng số bộ dữ liệu, số lượt xem, số lượt tải.

### 3. Chẩn đoán bằng Học máy (ML) 

Đây là chức năng cốt lõi của **ML Service (Python)**.

- **Xây dựng Mô hình Chẩn đoán:** Sử dụng dữ liệu đã thu thập được để xây dựng mô hình học máy (ví dụ: mô hình Hồi quy - Regression).
- **Chẩn đoán chỉ số:** Mô hình tập trung vào việc "chẩn đoán" hoặc dự báo các chỉ số chuyển đổi số, hoặc phân tích mức độ ảnh hưởng của các yếu tố (ví dụ: Internet, dịch vụ công) đến một chỉ số chung.
- **Cung cấp API cho ML:** Dịch vụ ML cung cấp API riêng để Frontend hoặc Backend có thể gọi và nhận về kết quả chẩn đoán.

### 4. Cung cấp API Mở & Nguồn mở 

Đây là chức năng của **Backend (Spring Boot)** và toàn bộ dự án.

- **Cung cấp REST API:** Backend Spring Boot cung cấp một endpoint RESTful cho phép các ứng dụng (như Frontend Nuxt.js) truy vấn dữ liệu được tích hợp.
- **Tài liệu hóa API:** Tích hợp Springdoc (OpenAPI 3) để tự động tạo tài liệu Swagger UI, phục vụ cho việc công bố tài liệu API trên Docusaurus và `README.md`.
- **Công khai Mã nguồn:** Toàn bộ mã nguồn dự án được quản lý công khai trên GitHub và đi kèm file `LICENSE` hợp lệ (Apache 2.0).
