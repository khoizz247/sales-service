# Báo cáo kiểm thử tải Sales Service trên Kaggle CPU

## Phạm vi và môi trường

- Lần chạy: `20260924T092228Z`; bắt đầu lúc `2026-09-24T09:24:39Z`.
- Phiên bản ứng dụng được đo: Git commit `d4176cb98df6c578210c5c2af6148c87eaf29bd1`.
- Kaggle CPU Notebook: 4 luồng CPU, 31,35 GiB RAM. Java 21.0.12.1, MySQL 8.0.46 và Locust 2.32.1.
- API, MySQL và Locust chạy trên cùng phiên Kaggle; Locust gọi API qua `localhost`. Các request tạo tài khoản và sản phẩm thử nghiệm được thực hiện trước mỗi pha, không tính vào số liệu đo.
- Tác vụ mục tiêu: 60% danh sách sản phẩm, 15% chi tiết sản phẩm, 10% đăng nhập, 10% xem đơn của tôi, 5% tạo đơn. Mỗi người dùng ảo chờ ngẫu nhiên 1–3 giây giữa các tác vụ.

## Kết quả

| Pha | Người dùng ảo | Thời gian | Request | Lỗi | RPS | p50 | p95 | p99 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Smoke | 5 | 60 giây | 145 | 0 | 2,46 | 10 ms | 140 ms | 150 ms |
| Baseline | 10 | 120 giây | 588 | 0 | 4,94 | 7 ms | 130 ms | 140 ms |
| Main | 30 | 300 giây | 4.410 | 0 | 14,72 | 6 ms | 130 ms | 140 ms |
| High | 60 | 300 giây | 8.764 | 0 | 29,34 | 5 ms | 130 ms | 150 ms |

Tổng cộng có **13.907 request được đo và 0 request lỗi**. Cả bốn pha có `exit_code = 0`. Khi tăng từ 5 lên 60 người dùng ảo (gấp 12), thông lượng tăng từ 2,46 lên 29,34 RPS (khoảng 11,9 lần). p95 tổng hợp giữ trong khoảng 130–140 ms và p99 trong khoảng 140–150 ms; không thấy điểm suy giảm rõ rệt trong dải tải đã thử. p50 giảm ở các pha sau có thể chịu ảnh hưởng của khởi động JVM/bộ nhớ đệm, không nên diễn giải là hệ thống nhanh hơn khi tải tăng.

Ở pha 60 người dùng ảo, thống kê theo endpoint:

| Endpoint | Request | p95 | p99 |
| --- | ---: | ---: | ---: |
| `GET /api/products` | 5.276 | 7 ms | 9 ms |
| `GET /api/products/{id}` | 1.310 | 7 ms | 9 ms |
| `POST /api/auth/login` | 861 | 150 ms | 170 ms |
| `GET /api/orders/me` | 859 | 18 ms | 24 ms |
| `POST /api/orders` | 458 | 52 ms | 81 ms |

Đăng nhập là tác vụ có độ trễ cao nhất trong nhóm endpoint này. p95 tổng hợp chịu ảnh hưởng của các request đăng nhập; không đại diện cho riêng API danh sách sản phẩm hoặc tạo đơn.

## Kiểm tra lỗi và kết luận

Các file `*_failures.csv` và `*_exceptions.csv` của cả bốn pha chỉ có dòng tiêu đề; log Locust không ghi nhận lỗi request. `api.log` không có dòng lỗi ứng dụng hay stack trace trong lần chạy. Ba cảnh báo `Trigger does not exist` xuất hiện khi Flyway chạy `DROP TRIGGER IF EXISTS` trên database mới; một cảnh báo khác nhắc rằng Hibernate không cần chỉ định `MySQLDialect` tường minh. Đây là cảnh báo khởi động, không phải request lỗi trong phép đo.

**Kết luận:** Với kịch bản và môi trường nêu trên, dịch vụ xử lý được mức thử nghiệm cao nhất là 60 người dùng ảo trong 5 phút, đạt 29,34 RPS, không ghi nhận request lỗi và chưa thấy độ trễ p95/p99 tổng hợp tăng đáng kể. Kết quả đáp ứng yêu cầu kiểm thử tải baseline của Pha 1; không phải cam kết hiệu năng production hoặc giới hạn chịu tải tối đa của hệ thống.

## Giới hạn phép đo

- Chỉ có một lần chạy trên một cấu hình Kaggle CPU; chưa kiểm tra tính lặp lại hoặc mức tải trên 60 người dùng ảo.
- API, MySQL và Locust dùng chung tài nguyên CPU/RAM, không đại diện cho triển khai tách máy hoặc lưu lượng qua mạng thực.
- Bốn pha chạy tuần tự trên cùng database, nên số bản ghi tăng dần. Mỗi người dùng có sản phẩm tồn kho riêng; phép đo này không kiểm tra tranh chấp khi nhiều người cùng mua một sản phẩm.
- Tỉ lệ tác vụ và thời gian chờ 1–3 giây là một kịch bản mô phỏng cụ thể; RPS và độ trễ có thể khác nếu lưu lượng thật có cơ cấu khác.

## Minh chứng

Số liệu và log gốc nằm trong `sales_load_results (2).zip`, thư mục `sales_load/20260924T092228Z/`: `metadata.json`, `summary.csv`, `*_stats.csv`, `*_failures.csv`, `*_exceptions.csv`, `*_stats_history.csv`, `*_report.html`, `*_locust.log` và `api.log`. Giữ ZIP gốc cùng bài nộp để người đánh giá có thể đối chiếu báo cáo này.
