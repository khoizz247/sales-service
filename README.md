# Sales Service

Backend cơ bản cho bài tập hệ thống bán hàng trực tuyến. Project mở trực tiếp bằng IntelliJ IDEA và có thể chạy ngay với H2 hoặc kết nối MySQL đã tạo bằng MySQL Workbench.

## Chức năng hiện có

- Đăng ký tài khoản `CUSTOMER`.
- ADMIN tạo tài khoản ADMIN mới kèm hồ sơ nhân viên, cần nhập lại mật khẩu quản trị.
- Người dùng đã đăng nhập có thể đổi mật khẩu tại `POST /api/users/me/password`.
- Đăng nhập và nhận JWT.
- `GET /api/users/me` yêu cầu xác thực.
- Xem danh sách sản phẩm có phân trang (`page`, `size`) và chi tiết sản phẩm.
- Tìm kiếm sản phẩm có phân trang tại `GET /api/products/search?q=&page=0&size=20`.
- Tìm đơn có phân trang theo mã/trạng thái cho admin và khách hàng.
- Admin thêm, cập nhật, chỉnh tồn kho và soft delete sản phẩm.
- Customer tạo đơn hàng, xem danh sách có phân trang và chi tiết đơn của mình; tự hủy đơn còn `PENDING`.
- Customer quản lý giỏ hàng và checkout; giá và tồn kho được kiểm tra lại khi checkout.
- Tạo đơn và trừ kho trong cùng transaction; hủy đơn sẽ hoàn kho.
- Admin xem danh sách có phân trang, chi tiết đơn và cập nhật trạng thái đơn.
- Danh mục sản phẩm, nhà cung cấp và liên kết sản phẩm–danh mục/nhà cung cấp.
- Customer quản lý địa chỉ của chính mình.
- Admin quản lý văn phòng và hồ sơ nhân viên; customer xem/sửa hồ sơ cá nhân, admin quản lý hạn mức tín dụng.
- Admin ghi nhận thanh toán thủ công, điều chỉnh kho và xem nhật ký kho; customer xem thanh toán, lịch sử trạng thái đơn của mình.
- Xác thực tập trung bằng Spring Security filter.
- Tài liệu Swagger/OpenAPI.
- Flyway tự quản lý migration MySQL, Hibernate chỉ `validate` schema.
- H2 có tài khoản admin mẫu phục vụ demo; MySQL chỉ bootstrap admin đầu tiên khi khai báo biến môi trường.

API tạo đơn trực tiếp vẫn giữ nguyên để tương thích. Giỏ hàng không giữ chỗ tồn kho; nếu tồn kho thay đổi trước checkout, API trả `409` và giỏ hàng vẫn được giữ lại.

## Kiến trúc

```text
api (Spring MVC)
    -> application/service (Java thuần)
        -> application/port/out (repository interface)
            -> infrastructure/persistence (Spring Data JPA cho dữ liệu lõi,
                                         JdbcTemplate cho các bảng mở rộng)

infrastructure/security (JWT + Spring Security filter)
domain/model (Java thuần)
```

Các package `domain` và `application` không phụ thuộc Spring Web hoặc JPA.

Xem [ERD bốn bảng lõi, kiến trúc và luồng trạng thái đơn hàng](docs/CORE_DESIGN.md). ERD toàn bộ schema và phân tích 1NF/2NF/3NF/BCNF nằm ở [database/NORMALIZATION.md](database/NORMALIZATION.md).

## Chạy bằng IntelliJ IDEA

1. Chọn **File > Open** và mở thư mục `sales-service`.
2. Chọn JDK 21 hoặc mới hơn cho project SDK.
3. Chờ IntelliJ tải dependency Maven.
4. Chạy lớp `vn.edu.sales.SalesServiceApplication`.
5. Mở Swagger tại <http://localhost:8080/swagger-ui.html>.

Mặc định ứng dụng dùng H2 trong bộ nhớ, nên không cần cài MySQL để chạy thử. Dữ liệu sẽ mất khi dừng ứng dụng. Bảng mở rộng của H2 được tạo bằng `src/main/resources/schema-h2.sql`; lịch sử trạng thái tự động bằng trigger chỉ có trên MySQL.

## Chạy bằng Maven Wrapper

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Kiểm thử:

```powershell
.\mvnw.cmd test
```

## Chạy API và MySQL bằng Docker Compose

Cần Docker Desktop đang chạy. Kiểm tra trong IntelliJ Terminal bằng `docker --version` và `docker compose version`. Nếu lệnh `docker` chưa tồn tại, xem [hướng dẫn cài Docker Desktop cho Windows](https://docs.docker.com/desktop/setup/install/windows-install/), mở Docker Desktop rồi mở lại Terminal.

```powershell
docker compose config --quiet
docker compose up --build
```

Mở một Terminal khác để xác minh:

```powershell
docker compose ps
docker compose exec mysql sh /usr/local/bin/mysql-healthcheck.sh
docker compose exec mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" sales_service -e "SELECT installed_rank,version,description,success FROM flyway_schema_history ORDER BY installed_rank"'
curl.exe http://localhost:8080/api/products
```

Kết quả mong đợi: cả `mysql` và `api` là `healthy`, Flyway có các version 1-3 và API trả danh sách sản phẩm JSON. Swagger ở <http://localhost:8080/swagger-ui.html>. Để dừng, nhấn `Ctrl+C` ở Terminal chạy `up`; dữ liệu trong Docker volume vẫn được giữ lại.

Docker dùng duy nhất schema `sales_service`: MySQL tạo database, sau đó API chạy Flyway `V1` (15 bảng, 2 view, 3 trigger), `V2` (dữ liệu mẫu) và `V3` (giỏ hàng và nhật ký cấp tài khoản). API chỉ dùng `ddl-auto=validate`, không để Hibernate tự tạo/sửa bảng. MySQL trong container dùng cổng 3306, từ Windows kết nối qua **localhost:3307**. Nếu cổng 8080 hoặc 3307 đã bận, đặt `API_HOST_PORT` hoặc `MYSQL_HOST_PORT` trong file `.env` cục bộ (Git bỏ qua).

MySQL 8.4 bật binary log mặc định; tài khoản `sales_user` không có quyền `SUPER` nên không thể tạo trigger trong `V1` nếu giữ nguyên cấu hình. Compose dành cho phát triển/CI đặt `log_bin_trust_function_creators=1` để Flyway tạo trigger mà không cấp `SUPER` cho ứng dụng. Cấu hình này nới lỏng kiểm tra an toàn của MySQL; khi triển khai production, dùng tài khoản migration do DBA quản lý và giữ cấu hình MySQL mặc định. Không dùng Compose mẫu này như cấu hình production nguyên trạng.

Để kiểm tra **khởi tạo từ database hoàn toàn rỗng** mà không đụng tới volume `sales-service` hiện có, mở một PowerShell mới tại thư mục project và chạy stack độc lập:

```powershell
$env:API_HOST_PORT = '18081'
$env:MYSQL_HOST_PORT = '3308'
docker compose -p sales-service-fresh up --build -d
docker compose -p sales-service-fresh ps
docker compose -p sales-service-fresh exec mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" sales_service -e "SELECT version,success FROM flyway_schema_history ORDER BY installed_rank; SHOW TABLES"'
curl.exe http://localhost:18081/api/products
docker compose -p sales-service-fresh down
```

Mong đợi cả hai container `healthy`, Flyway có V1–V3 thành công và 17 bảng nghiệp vụ (15 bảng ở V1, thêm 2 bảng ở V3); `SHOW TABLES` còn hiển thị thêm bảng nội bộ `flyway_schema_history`, tổng cộng 18. `down` không có `-v`, nên volume kiểm thử vẫn được giữ lại; không dùng `down -v` cho dữ liệu cần giữ.

Với volume cũ đã có schema chuẩn 15 bảng, Flyway tạo bản ghi baseline version 1 rồi chạy `V2` và `V3`; **sao lưu DB trước lần nâng cấp đầu tiên**. Nếu volume có schema khác hoặc chưa hoàn chỉnh, hãy nâng cấp schema cũ trước; không dùng baseline tự động để che lỗi. Không chạy `docker compose down -v` nếu còn dữ liệu cần giữ. Các migration đã áp dụng không được sửa nội dung; thay đổi mới phải là file `V4__...sql` trở đi.

MySQL **không tự tạo admin với mật khẩu mẫu**. Khi khởi tạo database hoàn toàn mới, tạo file `.env` cục bộ (không commit) với `BOOTSTRAP_ADMIN_EMAIL=...` và `BOOTSTRAP_ADMIN_PASSWORD=...` (12-72 ký tự, chữ hoa, chữ thường, số, ký tự đặc biệt). Bootstrap chỉ tạo tài khoản nếu bảng `users` đang trống và không bao giờ đặt lại mật khẩu. Sau đó ADMIN đăng nhập và dùng `POST /api/admin/employee-accounts` để tạo các tài khoản ADMIN khác kèm hồ sơ nhân viên; yêu cầu nhập lại `currentPassword`. Việc cấp tài khoản được ghi vào `admin_account_audit`; nhân viên nên đổi mật khẩu qua `POST /api/users/me/password`. Không dùng mật khẩu mẫu cho môi trường chia sẻ.

## Chạy với MySQL Workbench

1. Tạo database rỗng `sales_service` và cấp quyền cho user ứng dụng. **Không chạy `01_schema.sql`/`02_seed.sql` trên database mới**; Flyway sẽ tự chạy migration khi ứng dụng khởi động. Database cũ phải được sao lưu và có đủ schema chuẩn trước khi baseline.
2. Trong IntelliJ, mở **Run > Edit Configurations**.
3. Thêm biến môi trường `SPRING_PROFILES_ACTIVE=mysql`.
4. Thêm `DB_USERNAME`, `DB_PASSWORD` nếu khác mặc định. Với database rỗng, thêm `BOOTSTRAP_ADMIN_EMAIL` và `BOOTSTRAP_ADMIN_PASSWORD` để tạo admin đầu tiên.
5. Chạy lại `SalesServiceApplication`.

Profile `mysql` mặc định kết nối `jdbc:mysql://localhost:3306/sales_service` với tài khoản `sales_user`. Hibernate dùng `ddl-auto=validate`, nên ứng dụng chỉ kiểm tra schema thay vì tự ý sửa bảng.

## Tài khoản demo H2

```text
Email: admin@example.com
Password: Admin@123
Role: ADMIN
```

Tài khoản này chỉ tự tạo khi dùng H2 mặc định hoặc bật seed một cách tường minh. MySQL không tự tạo tài khoản mẫu. Không sử dụng tài khoản hoặc JWT secret mặc định khi triển khai thật.

## API chính

| Method | Endpoint | Quyền |
|---|---|---|
| POST | `/api/auth/register` | Công khai |
| POST | `/api/auth/login` | Công khai |
| GET | `/api/products?page=0&size=20` | Công khai |
| GET | `/api/products/search?q=&page=0&size=20` | Công khai |
| GET | `/api/products/{id}` | Công khai |
| GET | `/api/users/me` | Đã đăng nhập |
| POST | `/api/users/me/password` | Đã đăng nhập |
| GET, PUT | `/api/users/me/profile` | CUSTOMER |
| GET | `/api/admin/customers`, `/api/admin/customers/{userId}/profile` | ADMIN |
| PATCH | `/api/admin/customers/{userId}/credit-limit` | ADMIN |
| GET, POST | `/api/admin/offices` | ADMIN |
| GET, PUT | `/api/admin/offices/{id}` | ADMIN |
| GET, POST | `/api/admin/employees` | ADMIN |
| GET, PUT | `/api/admin/employees/{userId}` | ADMIN |
| POST | `/api/admin/employee-accounts` | ADMIN + mật khẩu hiện tại |
| GET | `/api/categories`, `/api/categories/{id}` | Công khai |
| GET | `/api/products/{id}/categories` | Công khai |
| POST, PUT | `/api/admin/categories[/{id}]` | ADMIN |
| POST, DELETE | `/api/admin/products/{id}/categories/{categoryId}` | ADMIN |
| GET | `/api/suppliers[/{id}]` | ADMIN |
| POST, PUT | `/api/admin/suppliers[/{id}]` | ADMIN |
| GET, PUT, DELETE | `/api/admin/products/{id}/suppliers[/{supplierId}]` | ADMIN |
| POST | `/api/products` | ADMIN |
| PUT | `/api/products/{id}` | ADMIN |
| PATCH | `/api/products/{id}/stock` | ADMIN |
| DELETE | `/api/products/{id}` | ADMIN |
| GET, POST, PUT, DELETE | `/api/users/me/addresses[/{id}]` | CUSTOMER |
| POST | `/api/orders` | CUSTOMER |
| GET, DELETE | `/api/cart` | CUSTOMER |
| PUT, DELETE | `/api/cart/items/{productId}` | CUSTOMER |
| POST | `/api/cart/checkout` | CUSTOMER |
| GET | `/api/orders/me?page=0&size=20` | CUSTOMER |
| GET | `/api/orders/me/search?code=&status=&page=0&size=20` | CUSTOMER |
| GET | `/api/orders/{id}` | Chủ đơn hàng |
| PATCH | `/api/orders/{id}/cancel` | Chủ đơn hàng, chỉ khi `PENDING` |
| GET | `/api/orders/{id}/history`, `/api/orders/{id}/payments` | Chủ đơn hàng |
| GET | `/api/admin/orders?page=0&size=20` | ADMIN |
| GET | `/api/admin/orders/search?code=&status=&page=0&size=20` | ADMIN |
| GET | `/api/admin/orders/{id}` | ADMIN |
| PATCH | `/api/admin/orders/{id}/status` | ADMIN |
| GET | `/api/admin/orders/{id}/history`, `/api/admin/orders/{id}/payments` | ADMIN |
| POST | `/api/admin/orders/{id}/payments` | ADMIN |
| PATCH | `/api/admin/payments/{id}/status` | ADMIN |
| GET | `/api/admin/products/{id}/inventory` | ADMIN |
| POST | `/api/admin/products/{id}/inventory/adjustments` | ADMIN |

Thử nhanh bằng Swagger: đăng nhập ADMIN và bấm **Authorize**, tạo văn phòng để lấy `officeId`, rồi gọi `POST /api/admin/employee-accounts`:

```json
{
  "currentPassword": "mật khẩu ADMIN đang đăng nhập",
  "fullName": "Nhân viên A",
  "email": "staff-a@example.com",
  "password": "New@Strong1234",
  "employeeCode": "EMP-A001",
  "officeId": 1,
  "jobTitle": "Nhân viên bán hàng",
  "hireDate": "2024-01-01"
}
```

Đăng nhập tài khoản mới rồi đổi mật khẩu bằng `POST /api/users/me/password` với `currentPassword` và `newPassword`. Customer thử `PUT /api/cart/items/{productId}` với `{"quantity":2}`, sau đó `POST /api/cart/checkout` với `recipientName`, `recipientPhone`, `shippingAddress`. Giỏ hàng không giữ chỗ tồn kho; checkout sẽ kiểm tra lại.

Thanh toán chỉ là **ghi nhận thủ công**, chưa kết nối cổng thanh toán thật. Khi chuyển sang `PAID`, tổng tiền đã thanh toán không được vượt tổng đơn. Đơn đã thanh toán phải hoàn tiền (`REFUNDED`) trước khi hủy. Tạo đơn ghi `SALE`, hủy đơn ghi `SALE_REVERSAL` vào `inventory_movements` trong cùng transaction.

Hồ sơ nhân viên chỉ gắn với tài khoản `ADMIN`; endpoint cấp tài khoản mới tạo user và hồ sơ trong cùng transaction. Đăng ký công khai chỉ tạo `CUSTOMER`. Xem [hướng dẫn thử API bằng Swagger](docs/SWAGGER_TEST_GUIDE.md).

[Bảng tham chiếu tất cả endpoint và JSON mẫu](docs/API_EXAMPLES.md) ghi rõ method, quyền, body và mã phản hồi mong đợi. Các endpoint GET/DELETE không nhận JSON body.

### Lấy token admin

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "Admin@123"
}
```

Trong Swagger, nhấn **Authorize** và dán trực tiếp giá trị `accessToken` (không thêm chữ `Bearer`).

### JSON tạo đơn mẫu

```json
{
  "recipientName": "Nguyễn Văn A",
  "recipientPhone": "0901234567",
  "shippingAddress": "123 Đường Mẫu, Hà Nội",
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}
```

## Cấu hình môi trường

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | rỗng | Đặt `mysql` để dùng cấu hình MySQL |
| `DB_URL` | H2 in-memory | JDBC URL; profile MySQL dùng database `sales_service` |
| `DB_USERNAME` | `sa` | Tài khoản DB |
| `DB_PASSWORD` | rỗng | Mật khẩu DB |
| `JWT_SECRET` | secret demo | Khóa Base64 tối thiểu 256 bit |
| `JWT_EXPIRATION_MS` | `86400000` | Thời hạn token |
| `SEED_ENABLED` | `true` | Bật dữ liệu demo |

## Kiểm thử tự động

Chạy bộ JUnit/MockMvc bằng `mvnw.cmd test` (Windows), `./mvnw test` (macOS/Linux), hoặc chọn Maven → Lifecycle → `test` trong IntelliJ. Các test dùng profile `test` và H2 riêng, không kết nối MySQL của thành viên trong nhóm. Unit test kiểm tra `ProductService`, `AuthService`, `OrderService`; integration test kiểm tra phân quyền `CUSTOMER`/`ADMIN`, JWT `401`/`403`, quản lý sản phẩm, tồn kho khi đặt/hủy đơn, hủy lặp, chuyển trạng thái `409`, tổng tiền và các API hồ sơ, địa chỉ, văn phòng, nhân viên, danh mục, nhà cung cấp, thanh toán. Xem [src/test/java/vn/edu/sales](src/test/java/vn/edu/sales).

Workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) chạy lại JUnit, dựng Docker Compose trên database rỗng và thử hai customer mua đồng thời toàn bộ tồn kho của một sản phẩm MySQL khi push/pull request tới `main`. Test H2 cũng có tình huống đồng thời. **Script MySQL này tiêu thụ tồn kho, chỉ chạy trên database dùng một lần.**

Kịch bản kiểm thử tải Kaggle CPU, Locust và cách xuất CSV/báo cáo nằm trong [performance/README.md](performance/README.md). Các file này là công cụ chuẩn bị; chỉ báo cáo kết quả tải sau khi nhóm đã chạy notebook trên Kaggle và tải kết quả thực về.

Để demo thủ công trong IntelliJ, mở [docs/demo.http](docs/demo.http) và bấm nút chạy cạnh từng request. Sửa email/mật khẩu ADMIN cho khớp tài khoản bootstrap của bạn, đăng nhập để lưu JWT, sau đó thử phân trang, tạo đơn, xem chi tiết và customer hủy đơn.

Có thể chạy thêm smoke test HTTP bằng [scripts/test-api.ps1](scripts/test-api.ps1) trên một phiên H2 **dành riêng cho test**:

```powershell
java -jar target/sales-service-0.0.1-SNAPSHOT.jar --server.port=18080
# Trong terminal PowerShell khác:
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-api.ps1 -BaseUrl http://localhost:18080
```

Script HTTP tạo sản phẩm, khách hàng và đơn hàng để kiểm tra các luồng cốt lõi; không chạy nó trên MySQL chứa dữ liệu thật.

## Giới hạn hiện tại

- Thanh toán chỉ được ghi nhận thủ công, không tích hợp nhà cung cấp thanh toán thật.
- Giỏ hàng không giữ chỗ tồn kho; lúc checkout vẫn phải kiểm tra lại.
- H2 không có trigger lịch sử trạng thái; kiểm tra lịch sử đầy đủ trên MySQL.
- Docker Compose dùng mật khẩu/secret mặc định chỉ để phát triển cục bộ; phải cấu hình `.env` riêng trước khi triển khai vào môi trường chia sẻ.

## Thiết kế MySQL

Các script MySQL Workbench **tham chiếu/khôi phục database cũ** nằm trong thư mục [`database`](database/README.md). Database mới dùng Flyway trong `src/main/resources/db/migration`:

1. `01_schema.sql`: tạo schema bán hàng chuẩn hóa, các ràng buộc và view kiểm tra.
2. `02_seed.sql`: thêm danh mục, nhà cung cấp, sản phẩm và số dư kho mẫu.
3. `03_create_local_user.sql`: tạo tài khoản MySQL phục vụ phát triển cục bộ.
4. `05_prepare_normalized_upgrade.sql`: chuẩn bị nâng cấp database bốn bảng hiện có.
5. `NORMALIZATION.md`: ERD mở rộng, phụ thuộc hàm và phân tích 1NF/2NF/3NF/BCNF.

Đọc `database/README.md` trước khi chạy script. Bốn bảng lõi dùng JPA; các bảng mở rộng đang có API dùng JDBC qua cổng dữ liệu ở tầng application.
