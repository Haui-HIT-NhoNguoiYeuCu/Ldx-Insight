---
sidebar_position: 8
title: Slide 8 - API mở & chia sẻ
---

# Tiêu chí 4: API mở & chia sẻ

## Hệ thống REST API công khai

- Namespace `/api/v1` với các nhóm endpoint: `auth`, `datasets`, `stats`, `ml`.
- **Swagger UI** tự động (Springdoc) tại `/swagger-ui/index.html`, giúp tra cứu nhanh.
- Mỗi endpoint được kiểm thử tự động và hiện đang hoạt động trên môi trường demo.

### Ví dụ gọi API

```bash
curl -X GET "https://api.ldx-insight.io/api/v1/datasets?category=chuyen-doi-so" \
     -H "Accept: application/json"
```

- Phản hồi trả về JSON chuẩn hóa: danh sách dataset, pagination, metadata (slug, topics, downloadCount).
- Hỗ trợ CORS cho các domain tin cậy (localhost, vercel.app, haui-hit-h2k.site).

> API mở giúp cộng đồng GovTech, báo chí, startup dễ dàng tích hợp dữ liệu vào sản phẩm của họ.
