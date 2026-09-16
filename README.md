# Sales Service

Backend cơ bản cho bài tập hệ thống bán hàng trực tuyến. Project mở trực tiếp bằng IntelliJ IDEA và có thể chạy ngay với cơ sở dữ liệu H2 trong bộ nhớ. Khi chạy bằng Docker Compose, ứng dụng sử dụng MySQL 8.4.

## Chức năng hiện có

- Đăng ký tài khoản `CUSTOMER`.
- Đăng nhập và nhận JWT.
- `GET /api/users/me` yêu cầu xác thực.
- Xem danh sách và chi tiết sản phẩm.
- Admin thêm và xóa sản phẩm.
- Xác thực tập trung bằng Spring Security filter.
- Tài liệu Swagger/OpenAPI.
- Dữ liệu mẫu và tài khoản admin phục vụ demo.

Giỏ hàng và đơn hàng chưa được triển khai trong bản base này.

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

## Chạy MySQL và API bằng Docker

Yêu cầu Docker Desktop:

```powershell
Copy-Item .env.example .env
docker compose up --build
```

API chạy tại <http://localhost:8080>. Dừng hệ thống bằng:

```powershell
docker compose down
```

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
| DELETE | `/api/products/{id}` | ADMIN |

### Lấy token admin

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "Admin@123"
}
```

Trong Swagger, nhấn **Authorize** và nhập token JWT. Nếu giao diện yêu cầu cả tiền tố thì nhập `Bearer <token>`.

## Cấu hình môi trường

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `DB_URL` | H2 in-memory | JDBC URL; Docker truyền URL MySQL |
| `DB_USERNAME` | `sa` | Tài khoản DB |
| `DB_PASSWORD` | rỗng | Mật khẩu DB |
| `JWT_SECRET` | secret demo | Khóa Base64 tối thiểu 256 bit |
| `JWT_EXPIRATION_MS` | `86400000` | Thời hạn token |
| `SEED_ENABLED` | `true` | Bật dữ liệu demo |

## Phần nên làm tiếp

1. API cập nhật sản phẩm và phân trang/tìm kiếm.
2. Giỏ hàng (`carts`, `cart_items`).
3. Đơn hàng (`orders`, `order_items`) và trừ tồn kho trong transaction.
4. Flyway migration thay cho `ddl-auto=update`.
5. Integration test cho xác thực và phân quyền.

## Thiết kế MySQL

Các script MySQL Workbench nằm trong thư mục [`database`](database/README.md):

1. `01_schema.sql`: tạo schema và bốn bảng chính.
2. `02_seed.sql`: thêm dữ liệu sản phẩm mẫu.
3. `03_create_local_user.sql`: tạo tài khoản MySQL phục vụ phát triển cục bộ.

Đọc `database/README.md` trước khi chạy script. Java entity sẽ được đồng bộ với schema này ở bước phát triển tiếp theo.
