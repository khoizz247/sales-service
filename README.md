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
- Xác thực tập trung bằng Spring Security filter.
- Tài liệu Swagger/OpenAPI.
- Dữ liệu mẫu và tài khoản admin phục vụ demo.

Giỏ hàng chưa được triển khai; customer tạo đơn trực tiếp từ danh sách sản phẩm và số lượng.

## Kiến trúc

```text
api (Spring MVC)
    -> application/service (Java thuần)
        -> application/port/out (repository interface)
            -> infrastructure/persistence (Spring Data JPA)

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

Mặc định ứng dụng dùng H2 trong bộ nhớ, nên không cần cài MySQL để chạy thử. Dữ liệu sẽ mất khi dừng ứng dụng.

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
| POST | `/api/products` | ADMIN |
| PUT | `/api/products/{id}` | ADMIN |
| PATCH | `/api/products/{id}/stock` | ADMIN |
| DELETE | `/api/products/{id}` | ADMIN |
| POST | `/api/orders` | CUSTOMER/ADMIN đã đăng nhập |
| GET | `/api/orders/me` | Đã đăng nhập |
| GET | `/api/orders/{id}` | Chủ đơn hàng |
| GET | `/api/admin/orders` | ADMIN |
| PATCH | `/api/admin/orders/{id}/status` | ADMIN |

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

## Phần nên làm tiếp

1. Phân trang và tìm kiếm sản phẩm/đơn hàng.
2. Giỏ hàng (`carts`, `cart_items`).
3. Flyway migration thay cho script chạy tay.
4. Integration test cho xác thực, phân quyền và tranh chấp tồn kho.

## Thiết kế MySQL

Các script MySQL Workbench nằm trong thư mục [`database`](database/README.md):

1. `01_schema.sql`: tạo schema bán hàng chuẩn hóa, các ràng buộc và view kiểm tra.
2. `02_seed.sql`: thêm danh mục, nhà cung cấp, sản phẩm và số dư kho mẫu.
3. `03_create_local_user.sql`: tạo tài khoản MySQL phục vụ phát triển cục bộ.
4. `05_prepare_normalized_upgrade.sql`: chuẩn bị nâng cấp database bốn bảng hiện có.
5. `NORMALIZATION.md`: ERD, phụ thuộc hàm và chứng minh 1NF/2NF/3NF/BCNF.

Đọc `database/README.md` trước khi chạy script. Java entity và repository đã được đồng bộ với schema này.
