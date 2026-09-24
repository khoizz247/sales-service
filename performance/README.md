# Kiểm thử tải trên Kaggle CPU

Kết quả chạy thực tế và phần phân tích đã kiểm tra CSV/log nằm trong [REPORT.md](REPORT.md). Báo cáo này ứng với commit ứng dụng được ghi trong báo cáo; ZIP kết quả gốc cần được giữ cùng bài nộp để đối chiếu.

Mở [kaggle_load_test.ipynb](kaggle_load_test.ipynb) trong **Kaggle Notebook mới**, chọn CPU và bật Internet trong Session options. Chạy lần lượt các cell hoặc chọn **Save & Run All**. Notebook clone `main` và ghi chính xác commit đã đo trong `metadata.json`; nếu đã thay đổi mã sau lần chạy, cần chạy lại để có kết quả của commit mới. Kaggle cho phép cài package từ notebook khi Internet bật và lưu output ở `/kaggle/working`.

Notebook cần quyền root, Ubuntu có gói `mysql-server` 8.x và `openjdk-21-jdk-headless`, cùng khả năng tải dependency Maven/PyPI. Nó sẽ dừng với lỗi rõ nếu không đáp ứng điều kiện này. Script không dùng H2 hay database từ máy cá nhân. Sau khi cài, script tạo `sales_service` rỗng, chạy Flyway V1-V3, khởi động API và MySQL trực tiếp trên cùng phiên CPU, rồi chạy Locust ở `localhost:18080`. Không cần Docker trong Kaggle.

Kịch bản [locustfile.py](locustfile.py) thực hiện đúng tỉ lệ dự kiến: 60% danh sách sản phẩm, 15% chi tiết sản phẩm, 10% đăng nhập, 10% danh sách đơn của người dùng và 5% tạo đơn. Trước mỗi pha, [kaggle_run.py](kaggle_run.py) chuẩn bị một Customer và một sản phẩm có tồn kho lớn riêng cho từng người dùng ảo. Các request chuẩn bị chạy xong trước khi Locust bắt đầu, nên không nằm trong thống kê và không tạo tải nền trong thời gian đo. File tạm chứa token/mật khẩu được xóa sau mỗi pha. Giỏ hàng không được đo vì không thuộc kịch bản gốc. Các pha chạy tuần tự trên cùng database nên số bản ghi tăng dần; ghi nhận đây là giới hạn phương pháp.

| Pha | Users | Thời gian |
|---|---:|---:|
| Smoke | 5 | 1 phút |
| Baseline | 10 | 2 phút |
| Tải chính | 30 | 5 phút |
| Tải cao | 60 | 5 phút, chỉ khi pha chính không có request lỗi |

Kết quả thực được tạo dưới `/kaggle/working/sales_load/<timestamp>/`:

- `metadata.json`: commit, thời gian UTC, cấu hình CPU/RAM, phiên bản Java/MySQL/Locust và các pha.
- `*_stats.csv`, `*_failures.csv`, `*_stats_history.csv`: kết quả gốc của Locust.
- `*_report.html`: biểu đồ và thống kê từng pha từ Locust.
- `summary.csv`, `REPORT.md`: bảng p50/p95/p99, RPS và lỗi do [report.py](report.py) tổng hợp.
- `api.log`, `*_locust.log`: log để phân tích lỗi.

Cell cuối tạo `/kaggle/working/sales_load_results.zip`; tải ZIP này từ phần **Output** của notebook. Kiểm tra các file `*_failures.csv` và `api.log`, ghi nhận nguyên nhân mọi lỗi 5xx/connection/timeout. Không đưa mật khẩu, JWT hoặc dữ liệu cá nhân vào báo cáo. Không tự điền số đo khi notebook chưa chạy thành công.

Để thử khả năng cài đặt trước, mở cell chạy chính và bật `LOAD_SMOKE_ONLY=1`, rồi chạy trong phiên Kaggle mới. Khi đã ổn, khởi tạo **phiên mới** và chạy đủ bốn pha. Nếu Kaggle thay đổi image khiến `apt` không cung cấp MySQL 8/Java 21, ghi lại lỗi và dùng Kaggle Dataset chứa binary phù hợp; không thay bằng MariaDB/H2 mà vẫn báo cáo là MySQL.

Tham khảo: [Kaggle Notebook documentation](https://www.kaggle.com/docs/notebooks), [Locust CSV configuration](https://docs.locust.io/en/latest/configuration.html).
