# Thử API bằng Swagger trên localhost

## 1. Khởi động và đăng nhập admin

1. Mở project `sales-service` bằng IntelliJ và chạy `SalesServiceApplication`. Để dùng MySQL, chọn Run > Edit Configurations và đặt `SPRING_PROFILES_ACTIVE=mysql`; tạo database rỗng `sales_service`, để Flyway tự chạy migration. **Không chạy `database/01_schema.sql` trên database mới.** Với database cũ, sao lưu trước và đọc `database/README.md`.
2. Mở <http://localhost:8080/swagger-ui.html>. Nếu đã dùng cổng khác, thay `8080` cho đúng.
3. Mở `POST /api/auth/login` > **Try it out**, nhập:

   ```json
   {"email":"admin@example.com","password":"Admin@123"}
   ```

4. Bấm **Execute**, sao chép `accessToken` trong response. Bấm **Authorize** ở đầu Swagger, dán **chỉ token**, không gõ thêm `Bearer`, rồi bấm Authorize và Close.
5. Thử `GET /api/users/me`. Response có `id`, `email`, `fullName`, `roles`. Ghi lại `id` admin để tạo hồ sơ nhân viên.

> Tài khoản demo trên chỉ tự tạo ở H2. MySQL mới cần bootstrap ADMIN đầu tiên qua `BOOTSTRAP_ADMIN_EMAIL` và `BOOTSTRAP_ADMIN_PASSWORD` như README; nếu dùng MySQL cũ thì đăng nhập bằng tài khoản đã có. Trên H2 dữ liệu sẽ mất mỗi lần dừng app; trên MySQL dữ liệu được giữ lại. Các mã `officeCode`, `employeeCode`, `sku` phải duy nhất; khi thử lại trên MySQL hãy chọn mã mới.

## 2. Văn phòng (`offices`) — token ADMIN

Mở `POST /api/admin/offices`, Try it out và nhập:

```json
{
  "officeCode": "HN01",
  "name": "Văn phòng Hà Nội",
  "phone": "0901234567",
  "email": "hn@example.com",
  "addressLine1": "1 Tràng Tiền",
  "city": "Hà Nội",
  "countryCode": "VN"
}
```

Bấm Execute. Kết quả mong đợi: `201`, có `id` và `status: "ACTIVE"`. Ghi `id` để dùng làm `officeId`. Sau đó thử `GET /api/admin/offices` và `GET /api/admin/offices/{id}`.

Để thử cập nhật, dùng `PUT /api/admin/offices/{id}` với body đầy đủ (không cần `officeCode`):

```json
{
  "name": "Văn phòng Hà Nội - cập nhật",
  "phone": "0901234567",
  "email": "hn@example.com",
  "addressLine1": "1 Tràng Tiền",
  "city": "Hà Nội",
  "countryCode": "VN",
  "status": "ACTIVE"
}
```

Nếu tạo lại mã `HN01`, server trả `409`. Văn phòng có nhân viên `ACTIVE` không thể đổi sang `INACTIVE` (`409`). API không có DELETE vì các bảng khác tham chiếu văn phòng.

## 3. Nhân viên (`employee_profiles`) — token ADMIN

`userId` phải là ID của tài khoản `ADMIN` đang hoạt động. Dùng `id` vừa lấy từ `GET /api/users/me`; `officeId` là ID ở bước 2. Mở `POST /api/admin/employees`:

```json
{
  "userId": 1,
  "employeeCode": "EMP001",
  "officeId": 1,
  "managerUserId": null,
  "jobTitle": "Quản trị viên",
  "extension": "101",
  "hireDate": "2024-01-01"
}
```

**Thay `userId` và `officeId` bằng ID thực tế.** Kết quả mong đợi: `201`, `status: "ACTIVE"`. Kiểm tra qua `GET /api/admin/employees` hoặc `GET /api/admin/employees/{userId}`.

Để thử `PUT /api/admin/employees/{userId}`, nhập tất cả trường có trong ví dụ sau; `employeeCode` không đổi bằng endpoint này:

```json
{
  "officeId": 1,
  "managerUserId": null,
  "jobTitle": "Quản trị hệ thống",
  "extension": "102",
  "hireDate": "2024-01-01",
  "status": "ACTIVE"
}
```

Không dùng ID khách hàng làm `userId`: server sẽ trả `404`. Mã nhân viên trùng trả `409`; không thể gán chính mình làm quản lý hoặc tạo vòng quản lý. Nếu chỉ có một admin mẫu, cứ để `managerUserId: null`.

## 4. Hồ sơ khách hàng (`customer_profiles`)

Trong Swagger, bấm **Authorize** > **Logout** để bỏ token admin. Dùng `POST /api/auth/register` tạo customer:

```json
{
  "fullName": "Nguyễn Văn A",
  "email": "customer-test@example.com",
  "password": "Test@1234"
}
```

Sao chép `accessToken` của customer và Authorize lại. `GET /api/users/me` cho biết `id` customer. `GET /api/users/me/profile` phải trả hồ sơ có `userId`, `phone: null` và `creditLimit: 0`.

Thử `PUT /api/users/me/profile`:

```json
{"phone":"0909999888","dateOfBirth":"2000-05-12"}
```

Kết quả mong đợi: `200`, số điện thoại và ngày sinh được cập nhật. Customer **không được sửa `creditLimit`** qua API này. Thử `GET /api/admin/offices` bằng token customer sẽ trả `403`.

Tiếp theo Logout token customer, đăng nhập admin và Authorize lại. Thử `GET /api/admin/customers` và `GET /api/admin/customers/{userId}/profile` với ID customer vừa ghi. Để đặt hạn mức, dùng `PATCH /api/admin/customers/{userId}/credit-limit`:

```json
{"creditLimit":1000000}
```

Kết quả mong đợi: `200`, `creditLimit: 1000000`. Giá trị âm trả `400`.

## 5. Kiểm tra nhanh các API đã làm ở đợt trước

| Token | Thao tác | Kết quả mong đợi |
|---|---|---|
| Không cần | `GET /api/categories`, `GET /api/products` | `200` |
| ADMIN | `POST /api/admin/categories`, `POST /api/admin/suppliers` | `201` |
| ADMIN | `POST /api/admin/products/{productId}/categories/{categoryId}` | `204` |
| ADMIN | `PUT /api/admin/products/{productId}/suppliers/{supplierId}` | `204` |
| CUSTOMER | `POST /api/users/me/addresses` rồi `GET /api/users/me/addresses` | `201`, `200` |
| CUSTOMER | `POST /api/orders` rồi `GET /api/orders/me` | `201`, `200` |
| ADMIN | `POST /api/admin/orders/{orderId}/payments` với `{"method":"COD","amount":890000}` | `201`, `PENDING` |
| ADMIN | `PATCH /api/admin/payments/{paymentId}/status` với `{"status":"PAID"}` | `200`, `PAID` |
| ADMIN | `GET /api/admin/products/{productId}/inventory` | `200`, có biến động `SALE` sau khi đặt đơn |
| CUSTOMER | `GET /api/orders/{orderId}/history` | `200`; MySQL ghi tự động bằng trigger |

`amount` thanh toán phải bằng hoặc nhỏ hơn tổng tiền đơn; thay `890000` bằng số phù hợp với đơn của bạn. Nếu đơn đã `PAID`, phải chuyển thanh toán sang `REFUNDED` trước khi hủy đơn. H2 không có trigger lịch sử trạng thái, nên endpoint history có thể trả `[]`; dùng MySQL để kiểm tra lịch sử này.

Mã phản hồi thường gặp: `401` thiếu/sai token; `403` đúng token nhưng sai quyền; `400` body không hợp lệ; `404` không tìm thấy ID; `409` trùng mã hoặc vi phạm quy tắc nghiệp vụ.
