---
sidebar_position: 4
title: Slide 4 - Kiến trúc hệ thống
---

# Sơ đồ kiến trúc hệ thống

## Mô hình tổng quan

```
Nguồn dữ liệu mở (Đồng Tháp, HCM, Đà Nẵng, Thanh Hóa)
        │ 1. API/CSV/XML
        ▼
OpenHub Crawler (Python 3, httpx, pandas, APScheduler)
        │ 2. JSON chuẩn hóa
        ▼
MongoDB Atlas (Replica Set)
        │ 3. Spring Data MongoDB
        ▼
Backend API (Spring Boot 3.3, Java 17, Spring Security, MapStruct, Swagger)
        │ 4. REST/JSON + JWT Cookie
        ▼
Frontend Dashboard (Nuxt 4, Vue 3, Pinia, Tailwind, Nuxt UI)
        │ 5. Người dùng cuối
        ▼
Cloudflare CDN + Vercel Edge + AWS EC2
```

## Công nghệ nguồn mở chính

- **Python stack**: httpx, pydantic, tenacity, pandas.
- **Java stack**: Spring Boot 3, Spring Security 6, JWT, Springdoc OpenAPI.
- **Frontend**: Nuxt 4, Vue 3, Pinia, Tailwind CSS 4, @nuxt/ui.
- **Hạ tầng**: MongoDB Atlas, AWS EC2, Vercel, Cloudflare.
