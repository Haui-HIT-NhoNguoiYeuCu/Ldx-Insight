---
sidebar_position: 9
title: Slide 9 - Nguồn mở & quản lý dự án
---

# Tiêu chí 5 & 6: Nguồn mở & cấu trúc kỹ thuật

## Quản lý dự án nguồn mở

- **GitHub**: https://github.com/Haui-HIT-NhoNguoiYeuCu/Ldx-Insight
- **Giấy phép**: Apache License 2.0 (LICENSE được công khai trong repo).
- **Release**: Đã phát hành `v1.0.0` (tag + changelog) trước thời gian nộp bài.
- **Tài liệu**: README.md hướng dẫn cài đặt, CONTRIBUTING.md mô tả quy trình đóng góp.

## Cấu trúc kỹ thuật & khả năng chạy thử

- Monorepo gồm 3 module chính: `backend/`, `frontend/`, `ai/`.
- Script cài đặt nhanh: `mvn spring-boot:run` (backend), `npm run dev` (frontend), `python scripts/run_agents.py` (crawler).
- Dockerfile (roadmap) đang chuẩn bị, hiện môi trường dev chạy stable trên Windows/Mac/Linux.
- CI/CD bảo đảm build pass trước khi merge vào `main`.

> Tinh thần nguồn mở: minh bạch, dễ đóng góp, khuyến khích cộng đồng GovTech tham gia mở rộng.
