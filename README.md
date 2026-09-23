# Sales Service

Backend cơ bản cho bài tập hệ thống bán hàng trực tuyến. Project mở trực tiếp bằng IntelliJ IDEA và có thể chạy ngay với H2 hoặc kết nối MySQL đã tạo bằng MySQL Workbench.

## Chức năng hiện có

- Đăng ký tài khoản `CUSTOMER`.
- Đăng nhập và nhận JWT.
- `GET /api/users/me` yêu cầu xác thực.
- Xem danh sách và chi tiết sản phẩm.
- Admin thêm, cập nhật, chỉnh tồn kho và soft delete sản phẩm.
- Customer tạo đơn hàng, xem danh sách và chi tiết đơn của mình.
- Tạo đơn và trừ kho trong cùng transaction; hủy đơn sẽ hoàn kho.
- Admin xem đơn và cập nhật trạng thái đơn.
- Danh mục sản phẩm, nhà cung cấp và liên kết sản phẩm–danh mục/nhà cung cấp.
- Customer quản lý địa chỉ của chính mình.
- Admin quản lý văn phòng và hồ sơ nhân viên; customer xem/sửa hồ sơ cá nhân, admin quản lý hạn mức tín dụng.
- Admin ghi nhận thanh toán thủ công, điều chỉnh kho và xem nhật ký kho; customer xem thanh toán, lịch sử trạng thái đơn của mình.
- Xác thực tập trung bằng Spring Security filter.
- Tài liệu Swagger/OpenAPI.
- Dữ liệu mẫu và tài khoản admin phục vụ demo.

Giỏ hàng chưa được triển khai; customer tạo đơn trực tiếp từ danh sách sản phẩm và số lượng.

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

## Chạy với MySQL Workbench

1. Database mới: chạy `database/01_schema.sql`, `database/02_seed.sql`, rồi `database/03_create_local_user.sql`. Database bốn bảng đang dùng: chạy `05_prepare_normalized_upgrade.sql`, chạy lại `01_schema.sql`, rồi `02_seed.sql`.
2. Trong IntelliJ, mở **Run > Edit Configurations**.
3. Thêm biến môi trường `SPRING_PROFILES_ACTIVE=mysql`.
4. Nếu không dùng tài khoản mẫu, thêm `DB_USERNAME` và `DB_PASSWORD` của bạn.
5. Chạy lại `SalesServiceApplication`.

Profile `mysql` mặc định kết nối `jdbc:mysql://localhost:3306/sales_service` với tài khoản `sales_user`. Hibernate dùng `ddl-auto=validate`, nên ứng dụng chỉ kiểm tra schema thay vì tự ý sửa bảng.

## Tài khoản demo

```text
Email: admin@example.com
Password: Admin@123
Role: ADMIN
```

Không sử dụng tài khoản hoặc JWT secret mặc định khi triển khai thật.

## API chính

| Method | Endpoint | Quyền |
|---|---|---|
| POST | `/api/auth/register` | Công khai |
| POST | `/api/auth/login` | Công khai |
| GET | `/api/products` | Công khai |
| GET | `/api/products/{id}` | Công khai |
| GET | `/api/users/me` | Đã đăng nhập |
| GET, PUT | `/api/users/me/profile` | CUSTOMER |
| GET | `/api/admin/customers`, `/api/admin/customers/{userId}/profile` | ADMIN |
| PATCH | `/api/admin/customers/{userId}/credit-limit` | ADMIN |
| GET, POST | `/api/admin/offices` | ADMIN |
| GET, PUT | `/api/admin/offices/{id}` | ADMIN |
| GET, POST | `/api/admin/employees` | ADMIN |
| GET, PUT | `/api/admin/employees/{userId}` | ADMIN |
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
| GET | `/api/orders/me` | CUSTOMER |
| GET | `/api/orders/{id}` | Chủ đơn hàng |
| GET | `/api/orders/{id}/history`, `/api/orders/{id}/payments` | Chủ đơn hàng |
| GET | `/api/admin/orders` | ADMIN |
| PATCH | `/api/admin/orders/{id}/status` | ADMIN |
| GET | `/api/admin/orders/{id}/history`, `/api/admin/orders/{id}/payments` | ADMIN |
| POST | `/api/admin/orders/{id}/payments` | ADMIN |
| PATCH | `/api/admin/payments/{id}/status` | ADMIN |
| GET | `/api/admin/products/{id}/inventory` | ADMIN |
| POST | `/api/admin/products/{id}/inventory/adjustments` | ADMIN |

Thanh toán chỉ là **ghi nhận thủ công**, chưa kết nối cổng thanh toán thật. Khi chuyển sang `PAID`, tổng tiền đã thanh toán không được vượt tổng đơn. Đơn đã thanh toán phải hoàn tiền (`REFUNDED`) trước khi hủy. Tạo đơn ghi `SALE`, hủy đơn ghi `SALE_REVERSAL` vào `inventory_movements` trong cùng transaction.

Hồ sơ nhân viên chỉ được tạo cho tài khoản `ADMIN` đã tồn tại. Đăng ký `CUSTOMER` sẽ tạo hồ sơ khách hàng; khách chỉ sửa số điện thoại/ngày sinh, còn admin mới sửa hạn mức. Xem [hướng dẫn thử API bằng Swagger](docs/SWAGGER_TEST_GUIDE.md).

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

Chạy bộ JUnit/MockMvc bằng `mvnw.cmd test` (Windows), `./mvnw test` (macOS/Linux), hoặc chọn Maven → Lifecycle → `test` trong IntelliJ. Các test dùng profile `test` và H2 riêng, không kết nối MySQL của thành viên trong nhóm. Bộ test bao phủ phân quyền `CUSTOMER`/`ADMIN`, JWT `401`/`403`, quản lý sản phẩm, tồn kho khi đặt/hủy đơn, hủy lặp, chuyển trạng thái `409`, tổng tiền và các API hồ sơ, địa chỉ, văn phòng, nhân viên, danh mục, nhà cung cấp, thanh toán. Xem [SalesApiIntegrationTest.java](src/test/java/vn/edu/sales/SalesApiIntegrationTest.java).

Có thể chạy thêm smoke test HTTP bằng [scripts/test-api.ps1](scripts/test-api.ps1) trên một phiên H2 **dành riêng cho test**:

```powershell
java -jar target/sales-service-0.0.1-SNAPSHOT.jar --server.port=18080
# Trong terminal PowerShell khác:
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-api.ps1 -BaseUrl http://localhost:18080
```

Script HTTP tạo sản phẩm, khách hàng và đơn hàng để kiểm tra các luồng cốt lõi; không chạy nó trên MySQL chứa dữ liệu thật.

## Phần nên làm tiếp

1. Cấp tài khoản `ADMIN` mới qua một quy trình an toàn để tạo nhiều nhân viên; hiện chỉ có tài khoản admin mẫu hoặc tài khoản thêm sẵn trong DB.
2. Bổ sung `created_by_user_id` cho biến động kho phát sinh từ API sản phẩm cũ (hiện trường này là `NULL`).
3. Phân trang/tìm kiếm, giỏ hàng và Flyway migration thay cho script chạy tay.
4. Mở rộng integration test cho nhiều yêu cầu đồng thời trên cùng sản phẩm và các nhánh lỗi ít gặp.

## Thiết kế MySQL

Các script MySQL Workbench nằm trong thư mục [`database`](database/README.md):

1. `01_schema.sql`: tạo schema bán hàng chuẩn hóa, các ràng buộc và view kiểm tra.
2. `02_seed.sql`: thêm danh mục, nhà cung cấp, sản phẩm và số dư kho mẫu.
3. `03_create_local_user.sql`: tạo tài khoản MySQL phục vụ phát triển cục bộ.
4. `05_prepare_normalized_upgrade.sql`: chuẩn bị nâng cấp database bốn bảng hiện có.
5. `NORMALIZATION.md`: ERD, phụ thuộc hàm và chứng minh 1NF/2NF/3NF/BCNF.

Đọc `database/README.md` trước khi chạy script. Bốn bảng lõi dùng JPA; các bảng mở rộng đang có API dùng JDBC qua cổng dữ liệu ở tầng application.
