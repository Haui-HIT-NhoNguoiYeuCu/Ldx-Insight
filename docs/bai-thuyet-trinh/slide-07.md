---
sidebar_position: 7
title: Slide 7 - Ứng dụng học máy
---

# Tiêu chí 3: Ứng dụng học máy

## Mô hình dự đoán chỉ số chuyển đổi số (CĐS Score)

- **Thuật toán**: RandomForestRegressor (scikit-learn) – chọn vì khả năng xử lý dữ liệu ít chiều và tương tác phi tuyến.
- **Tập dữ liệu huấn luyện**: Tổng hợp từ 3 nguồn open data, bao gồm các biến như số dịch vụ công trực tuyến, số thuê bao băng rộng, tỷ lệ sử dụng dịch vụ số.
- **Quy trình**:
  1. Tiền xử lý (normalize, xử lý missing value).
  2. Chia train/test 80/20, cross-validation 5-fold.
  3. Huấn luyện và xuất mô hình phục vụ API nội bộ `/api/v1/ml/diagnosis`.
- **Kết quả**:
  - MAE: 2.3 điểm; R²: 0.79 trên tập test.
  - Dashboard hiển thị heatmap dự báo CĐS trong 6 tháng tới, cảnh báo các tỉnh có xu hướng giảm.

> Mô hình giúp nhà quản lý chủ động lập kế hoạch hỗ trợ địa phương chậm tiến độ.
