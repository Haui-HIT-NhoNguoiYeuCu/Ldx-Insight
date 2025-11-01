# Hướng dẫn đóng góp cho Ldx-Insight

Chúng tôi rất vui vì bạn quan tâm đến việc đóng góp cho Ldx-Insight
! Mọi đóng góp, dù lớn hay nhỏ, đều được chào đón.

Tài liệu này là một hướng dẫn giúp bạn tham gia vào dự án một cách suôn sẻ.

## Mục lục

- [Cách thức đóng góp](#cách-thức-đóng-góp)
  - [Báo cáo lỗi (Bugs)](#báo-cáo-lỗi-bugs)
  - [Đề xuất tính năng (Features)](#đề-xuất-tính-năng-features)
  - [Gửi Pull Request (PR)](#gửi-pull-request-pr)
- [Hướng dẫn cài đặt để phát triển](#hướng-dẫn-cài-đặt-để-phát-triển)
- [Thỏa thuận Giấy phép Đóng góp](#thỏa-thuận-giấy-phép-đóng-góp)

## Cách thức đóng góp

Bạn có thể đóng góp theo nhiều cách khác nhau, không chỉ là viết code.

### Báo cáo lỗi (Bugs)

Nếu bạn tìm thấy một lỗi, vui lòng kiểm tra xem lỗi đó đã được báo cáo trong [mục Issues](https://github.com/Haui-HIT-NhoNguoiYeuCu/Ldx-Insight/issues) hay chưa.

Nếu chưa, hãy tạo một issue mới với các thông tin sau:

1.  **Tiêu đề rõ ràng:** Mô tả ngắn gọn về lỗi.
2.  **Mô tả chi tiết:**
    - Các bước để tái hiện lại lỗi (steps to reproduce).
    - Kết quả bạn mong đợi.
    - Kết quả thực tế bạn nhận được.
3.  **Môi trường:** (Ví dụ: phiên bản hệ điều hành, phiên bản Java, phiên bản Python, trình duyệt).

### Đề xuất tính năng (Features)

Bạn có ý tưởng tuyệt vời cho dự án? Chúng tôi rất muốn nghe!
Hãy tạo một issue mới trong [mục Issues](https://github.com/Haui-HIT-NhoNguoiYeuCu/Ldx-Insight/issues) và mô tả đề xuất của bạn. Hãy giải thích lý do tại sao tính năng này hữu ích và nó sẽ hoạt động như thế nào.

### Gửi Pull Request (PR)

Nếu bạn muốn đóng góp mã nguồn, đây là quy trình chuẩn:

1.  **Fork** kho chứa (repository) này về tài khoản GitHub của bạn.
2.  **Clone** kho chứa bạn đã fork về máy cá nhân:
    ```bash
    git clone https://github.com/Haui-HIT-NhoNguoiYeuCu/Ldx-Insight.git
    ```
3.  Tạo một **nhánh (branch)** mới cho tính năng hoặc bản sửa lỗi của bạn:
    ```bash
    git checkout -b ten-nhanh-cua-ban (ví dụ: feat/them-tinh-nang-abc)
    ```
4.  Thực hiện các thay đổi của bạn và **commit** chúng với một thông điệp rõ ràng:
    ```bash
    git commit -m "Feat: Thêm tính năng ABC"
    ```
5.  **Push** nhánh của bạn lên kho chứa đã fork:
    ```bash
    git push origin ten-nhanh-cua-ban
    ```
6.  Mở một **Pull Request (PR)** từ nhánh của bạn trên GitHub. Hãy mô tả rõ ràng những gì bạn đã thay đổi và tại sao.

## Thỏa thuận Giấy phép Đóng góp

Bằng cách đóng góp cho Ldx-Insight, bạn đồng ý rằng các đóng góp của mình sẽ được cấp phép theo [Giấy phép Apache 2.0](LICENSE) của dự án.
