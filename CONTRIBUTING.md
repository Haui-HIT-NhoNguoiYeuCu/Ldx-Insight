# Hướng dẫn đóng góp cho Ldx-Insight

Chúng tôi rất vui vì bạn quan tâm đến việc đóng góp cho Ldx-Insight!
Mọi đóng góp, dù lớn hay nhỏ, đều được chào đón.

Tài liệu này là một hướng dẫn giúp bạn tham gia vào dự án một cách suôn sẻ.

## Mục lục

- [Cách thức đóng góp](#cách-thức-đóng-góp)
  - [Báo cáo lỗi (Bugs)](#báo-cáo-lỗi-bugs)
  - [Đề xuất tính năng (Features)](#đề-xuất-tính-năng-features)
  - [Gửi Pull Request (PR)](#gửi-pull-request-pr)
- [Hướng dẫn cài đặt để phát triển](#hướng-dẫn-cài-đặt-để-phát-triển)
- [Giấy phép & Bản quyền](#giấy-phép--bản-quyền)
  - [Cam kết cấp phép khi đóng góp](#cam-kết-cấp-phép-khi-đóng-góp)
  - [Header bản quyền trong file nguồn](#header-bản-quyền-trong-file-nguồn)
  - [SPDX (tuỳ chọn)](#spdx-tuỳ-chọn)
  - [NOTICE & Attribution](#notice--attribution)
  - [Yêu cầu theo Apache-20 mục-4b](#yêu-cầu-theo-apache-20-mục-4b)
  - [Phụ thuộc bên thứ ba](#phụ-thuộc-bên-thứ-ba)
- [Script Python tự động chèn header](#script-python-tự-động-chèn-header)

## Cách thức đóng góp

Bạn có thể đóng góp theo nhiều cách khác nhau, không chỉ là viết code.

### Báo cáo lỗi (Bugs)

Nếu bạn tìm thấy một lỗi, vui lòng kiểm tra xem lỗi đó đã được báo cáo trong [mục Issues](https://github.com/Haui-HIT-NhoNguoiYeuCu/Ldx-Insight/issues) hay chưa.

Nếu chưa, hãy tạo một issue mới với các thông tin sau:

1. **Tiêu đề rõ ràng:** Mô tả ngắn gọn về lỗi.
2. **Mô tả chi tiết:**
   - Các bước để tái hiện lại lỗi (steps to reproduce).
   - Kết quả bạn mong đợi.
   - Kết quả thực tế bạn nhận được.
3. **Môi trường:** (Ví dụ: phiên bản hệ điều hành, phiên bản Java, phiên bản Python, trình duyệt).

### Đề xuất tính năng (Features)

Bạn có ý tưởng tuyệt vời cho dự án? Chúng tôi rất muốn nghe!  
Hãy tạo một issue mới trong [mục Issues](https://github.com/Haui-HIT-NhoNguoiYeuCu/Ldx-Insight/issues) và mô tả đề xuất của bạn. Hãy giải thích lý do tại sao tính năng này hữu ích và nó sẽ hoạt động như thế nào.

### Gửi Pull Request (PR)

Nếu bạn muốn đóng góp mã nguồn, đây là quy trình chuẩn:

1. **Fork** kho chứa (repository) này về tài khoản GitHub của bạn.
2. **Clone** kho chứa bạn đã fork về máy cá nhân:
   ```bash
   git clone https://github.com/Haui-HIT-NhoNguoiYeuCu/Ldx-Insight.git
   ```
3. Tạo một **nhánh (branch)** mới cho tính năng hoặc bản sửa lỗi của bạn:
   ```bash
   git checkout -b ten-nhanh-cua-ban  # ví dụ: feat/them-tinh-nang-abc
   ```
4. Thực hiện các thay đổi của bạn và **commit** chúng với một thông điệp rõ ràng:
   ```bash
   git commit -m "feat: thêm tính năng ABC"
   ```
5. **Push** nhánh của bạn lên kho chứa đã fork:
   ```bash
   git push origin ten-nhanh-cua-ban
   ```
6. Mở một **Pull Request (PR)** từ nhánh của bạn trên GitHub. Hãy mô tả rõ ràng những gì bạn đã thay đổi và tại sao.

> **Lưu ý:** Hãy đảm bảo code đã qua lint/test (nếu có), và bám sát style guide của module bạn sửa.

---

## Hướng dẫn cài đặt để phát triển

_(tuỳ project của bạn, bổ sung các bước cài đặt môi trường, build, chạy test, etc.)_

---

## Giấy phép & Bản quyền

Dự án được phát hành theo **Apache License 2.0**.

### Cam kết cấp phép khi đóng góp

Bằng cách đóng góp cho Ldx-Insight, **bạn đồng ý rằng đóng góp của mình sẽ được cấp phép theo [Apache License 2.0](LICENSE)** của dự án.  
Bạn xác nhận rằng bạn có quyền cấp phép cho phần đóng góp đó (tự viết, hoặc được ủy quyền, hoặc tuân theo giấy phép tương thích).

### Header bản quyền trong file nguồn

Vui lòng thêm header ở đầu **mỗi file mã nguồn** mới (hoặc đảm bảo đã có). Thay bằng thông tin của dự án:

**Năm:** 2025 **Chủ sở hữu:** Haui.HIT - H2K

- **Java/Kotlin**

  ```java
  /*
   * Copyright 2025 Haui.HIT - H2K
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   *     http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */
  ```

- **TypeScript/JavaScript**

  ```ts
  /**
   * Copyright 2025 Haui.HIT - H2K
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * http://www.apache.org/licenses/LICENSE-2.0
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   */
  ```

- **CSS/SCSS**

  ```css
  /*!
   * Copyright 2025 Haui.HIT - H2K
   * Licensed under the Apache License, Version 2.0
   * http://www.apache.org/licenses/LICENSE-2.0
   */
  ```

- **XML/HTML**

  ```xml
  <!--
    Copyright 2025 Haui.HIT - H2K
    Licensed under the Apache License, Version 2.0
    http://www.apache.org/licenses/LICENSE-2.0
  -->
  ```

- **YAML**

  ```yaml
  # Copyright 2025 Haui.HIT - H2K
  # Licensed under the Apache License, Version 2.0
  # http://www.apache.org/licenses/LICENSE-2.0
  ```

- **Markdown/MDX** (dùng comment HTML ở đầu file)
  ```md
  <!--
    Copyright 2025 Haui.HIT - H2K
    Licensed under the Apache License, Version 2.0
    http://www.apache.org/licenses/LICENSE-2.0
  -->
  ```

> **Không chèn header** vào định dạng **không hỗ trợ comment** như JSON, CSV, ảnh, binary. Chỉ cần `LICENSE/NOTICE` ở gốc.

### SPDX (tuỳ chọn)

Bạn có thể thêm một dòng SPDX (ngắn gọn, hỗ trợ tooling), ví dụ:

```
SPDX-License-Identifier: Apache-2.0
```

Có thể đặt ngay dòng đầu (trước/hoặc cùng với header).

### NOTICE & Attribution

- File `NOTICE` ở thư mục gốc nên chứa attribution cơ bản, ví dụ:
  ```
  Ldx-Insight
  Copyright (c) 2025 Haui.HIT - H2K
  This product includes software developed by the Ldx-Insight contributors.
  ```
- Nếu bạn thêm nội dung cần ghi công (third-party notices), hãy bổ sung vào `NOTICE` theo yêu cầu giấy phép của bên thứ ba.

### Yêu cầu theo Apache-2.0 mục 4(b)

Nếu bạn **sửa đổi file**, hãy đảm bảo file đó có **thông báo nổi bật** rằng bạn đã thay đổi (ví dụ ở đầu file hoặc trong phần changelog/commit):

> `// Modified by <your name/org> on <YYYY-MM-DD>: <mô tả ngắn thay đổi>`

Điều này giúp người nhận biết rõ phần nào đã thay đổi, phù hợp **mục 4(b)** của Apache-2.0.

### Phụ thuộc bên thứ ba

- Chỉ thêm thư viện/phần mềm có giấy phép **tương thích** (Apache-2.0, MIT, BSD, v.v.).
- Nếu giấy phép yêu cầu attribution/NOTICE, hãy cập nhật `NOTICE`.
- Không đưa mã có giấy phép **không tương thích** (ví dụ GPLv3) vào repo trừ khi đã thảo luận và đồng ý công khai phạm vi, rủi ro.

---

## Script Python tự động chèn header

Chúng tôi cung cấp script **`tools/add_license_headers.py`** để tự động chèn/đồng bộ header vào các file nguồn.

**Cách chạy:**

```bash
python tools/add_license_headers.py   --root .   --year 2025   --owner "Haui.HIT - H2K"   --spdx
```

**Mặc định script sẽ:**

- Chèn header cho: `.java, .kt, .ts, .tsx, .js, .jsx, .css, .scss, .xml, .html, .yml, .yaml, .md, .mdx, .sh`
- Bỏ qua: `node_modules`, `build`, `dist`, `.git`, `target`, `out`, `*.min.css`, `*.min.js`
- Không đụng đến: `json`, `csv`, ảnh, binary.

Xem mã nguồn script trong thư mục `tools/` để tuỳ chỉnh pattern mở rộng hoặc nội dung header.
