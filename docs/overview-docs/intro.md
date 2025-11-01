---
sidebar_position: 1
title: Giới Thiệu Tổng Quan
---

**Ldx-Insight** là một nền tảng tích hợp dữ liệu mở, được xây dựng theo kiến trúc 3 lớp, với sứ mệnh trở thành cầu nối tin cậy giữa các nguồn dữ liệu thô và các ứng dụng phân tích, chẩn đoán giá trị.

---

## 1. Vấn đề Đặt ra

Theo chủ đề của cuộc thi, trong tiến trình chuyển đổi số, dữ liệu mở của Việt Nam tuy đã được công bố nhưng vẫn còn nhiều thách thức:

- **Phân mảnh & Thiếu chuẩn:** Dữ liệu tồn tại ở khắp nơi (data.gov.vn, opendata.mic.gov.vn, các tỉnh thành), không tuân theo một quy chuẩn chung, khó kết hợp và đối chiếu.
- **Khó khai thác:** Dữ liệu thường được công bố ở các định dạng khác nhau, thiếu API đồng bộ, khó để máy móc đọc hiểu và tích hợp tự động.
- **Thiếu công cụ phân tích:** Việc khai thác dữ liệu đòi hỏi nỗ lực kỹ thuật, làm chậm quá trình xây dựng các công cụ phân tích, trực quan hóa và chẩn đoán.

## 2. Giải pháp của Ldx-Insight

Ldx-Insight được thiết kế để giải quyết những vấn đề trên bằng cách xây dựng một hệ thống tích hợp dữ liệu tập trung.

> Nền tảng tập trung vào việc **thu thập**, **chuẩn hóa**, và **cung cấp dữ liệu** thông qua REST API, giúp biến những bộ dữ liệu tĩnh thành các tài nguyên sống, sẵn sàng cho Dashboard và các mô hình học máy.

Chúng tôi tập trung vào việc chuẩn hóa dữ liệu về **JSON**, lưu trữ trên **MongoDB**, và cung cấp **REST API** (qua Spring Boot) cùng **Dashboard trực quan** (bằng Nuxt.js) để dễ dàng truy cập và phân tích.

## 3. Các Nguyên tắc Cốt lõi

Hoạt động của Ldx-Insight dựa trên bốn nguyên tắc chính:

- **Thu thập và Chuẩn hóa:** Xây dựng một kho dữ liệu thống nhất (Data Hub), nơi dữ liệu từ nhiều nguồn được script Python thu thập và **chuẩn hóa về định dạng JSON** lưu trữ trong MongoDB.
- **Cung cấp API Mở:** Cung cấp **REST API thống nhất** (phát triển bằng Spring Boot) giúp các nhà phát triển (ví dụ: Frontend Nuxt.js) giảm thiểu thời gian và công sức tích hợp dữ liệu.
- **Phân tích và Chẩn đoán:** Không chỉ lưu trữ, dự án còn xây dựng các mô hình học máy (ML) để **"chẩn đoán" và phân tích** các chỉ số chuyển đổi số, tăng giá trị cho dữ liệu thô.
- **Nguồn mở và Minh bạch:** Toàn bộ mã nguồn được công khai trên GitHub, tuân thủ **Giấy phép Apache 2.0**, chào đón mọi sự đóng góp từ cộng đồng.

## 4. Tầm nhìn

Ldx-Insight được kỳ vọng trở thành một công cụ hữu ích cho việc khai thác và ứng dụng dữ liệu mở, phục vụ trực tiếp cho mục tiêu **chuyển đổi số địa phương**, đặc biệt trong các lĩnh vực như **phân tích chỉ số ICT, y tế, giáo dục, hay môi trường**.
