---
sidebar_position: 1
title: Giới Thiệu Tổng Quan Backend
---

Phần **Backend** của dự án **Ldx-Insight** là một ứng dụng **Spring Boot 3.3.1** nguyên khối, được xây dựng theo **Kiến trúc 3 Lớp (3-Layer Architecture)**.

Đây là lõi trung tâm, chịu trách nhiệm xử lý toàn bộ logic nghiệp vụ, quản lý dữ liệu (thu thập từ Data Collector), và cung cấp REST API cho các bên tiêu thụ (ví dụ: Frontend Nuxt.js và Dịch vụ ML).

---

## 1. Mục tiêu & Công nghệ 🎯

- **Kiến trúc 3 Lớp Rõ Ràng:** Phân tách ứng dụng thành 3 lớp logic chính:
  1.  **Presentation Layer (Controller):** Tiếp nhận HTTP request và trả về response.
  2.  **Business Layer (Service):** Xử lý logic nghiệp vụ, tính toán.
  3.  **Data Access Layer (Repository):** Tương tác trực tiếp với cơ sở dữ liệu.
- **Cung cấp REST API:** Xây dựng các endpoint RESTful ổn định, hiệu quả để quản lý, tìm kiếm, và thống kê các bộ dữ liệu.
- **Công nghệ lõi:**
  - **Framework:** Spring Boot 3.3.1
  - **Ngôn ngữ:** Java 17
  - **Build Tool:** Maven
  - **Cơ sở dữ liệu:** MongoDB (sử dụng Spring Data MongoDB).
- **Bảo mật:** Tích hợp Spring Security 6 để quản lý xác thực và phân quyền (ví dụ: JWT).
- **Tài liệu hóa API:** Tích hợp Springdoc (OpenAPI 3) để tự động tạo tài liệu Swagger UI.

---

## 2. Thiết kế hệ thống (Kiến trúc 3 Lớp) 🏛️

Thay vì chia thành nhiều dịch vụ nhỏ (microservices), toàn bộ ứng dụng được đóng gói và chạy như một tiến trình duy nhất. Dữ liệu từ Data Collector (Python) sẽ được ghi trực tiếp vào MongoDB, và Backend Spring Boot sẽ đọc/ghi trên CSDL này.

### Luồng xử lý dữ liệu:

1.  **Frontend (Nuxt.js)** gọi một API (ví dụ: `GET /api/v1/datasets`).
2.  **Controller Layer** (`DatasetController`) tiếp nhận request.
3.  Controller gọi **Service Layer** (`DatasetService`).
4.  Service Layer thực thi logic nghiệp vụ và gọi **Repository Layer** (`DatasetRepository`).
5.  Repository Layer sử dụng Spring Data MongoDB để truy vấn CSDL.
6.  Dữ liệu được trả về theo luồng ngược lại (Repository -> Service -> Controller -> Frontend).


## 3. Cấu trúc dự án Backend (Gói) 📁

Dự án sẽ tuân theo cấu trúc gói (package) tiêu chuẩn của Spring Boot để phân tách các lớp:

```
backend/
├── src/main/java/io/ldxinsight/
│   ├── controller/    # Lớp Presentation (API Endpoints, ví dụ: DatasetController)
│   ├── service/       # Lớp Business (Logic nghiệp vụ, ví dụ: DatasetService)
│   ├── repository/    # Lớp Data Access (Spring Data MongoDB, ví dụ: DatasetRepository)
│   ├── model/         # (hoặc document) - Các đối tượng (POJO) ánh xạ với MongoDB
│   ├── config/        # Cấu hình (ví dụ: SecurityConfig, OpenApiConfig)
│   ├── dto/           # Data Transfer Objects (Request/Response)
│   ├── exception/     # Xử lý Exception tập trung (GlobalExceptionHandler)
│   └── LdxInsightBackendApplication.java # File chạy chính
│
├── src/main/resources/
│   ├── static/
│   ├── templates/
│   └── application.properties # (hoặc .yml) Cấu hình DB, server port...
│
└── pom.xml            # File Maven quản lý dependencies (Spring Web, Data MongoDB, Security...)
```

## 4. Cài đặt & Chạy dự án Backend 🚀

**Yêu cầu:**

- **Java Development Kit (JDK):** ≥ 17
- **Apache Maven:** ≥ 3.8.x
- **MongoDB:** Đã được cài đặt và đang chạy (ví dụ: trên `localhost:27017`)

### Cài đặt

```bash
# Clone repository (nếu chưa có)
git clone https://github.com/TEN-NHOM-CUA-BAN/ldx-insight.git
cd ldx-insight/backend
```

### Cấu hình

Mở tệp `backend/src/main/resources/application.properties` và đảm bảo bạn đã cấu hình kết nối MongoDB:

```properties
# Cấu hình cổng server (mặc định là 8080)
server.port=8080

# Cấu hình kết nối MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/ldx-insight-db
```

### Chạy dự án (Chế độ phát triển)

```bash
# Đảm bảo bạn đang ở thư mục backend/
# Build và chạy ứng dụng Spring Boot
mvn spring-boot:run
```

### Truy cập ứng dụng

- **API Base:** 👉 http://localhost:8080
- **API Documentation (Swagger UI):** 👉 http://localhost:8080/swagger-ui.html
