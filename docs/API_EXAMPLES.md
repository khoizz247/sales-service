# Tham chiếu API và JSON mẫu

Mở <http://localhost:8080/swagger-ui/index.html>. Đăng nhập bằng `POST /api/auth/login`, sao chép `accessToken` vào nút **Authorize** (không gõ thêm `Bearer`). Thay `{id}`, `{productId}`, `{orderId}`, `{userId}`... bằng ID có thật từ response trước đó. Các request `GET` và `DELETE`, cũng như thao tác gán/xóa quan hệ bằng URL, **không có JSON body**. `ADMIN`/`CUSTOMER` trong bảng là quyền bắt buộc; `PUBLIC` không cần token. Với MySQL, tạo ADMIN đầu tiên theo README; tài khoản demo bên dưới chỉ dùng cho H2.

## Danh sách endpoint

| Method | URL mẫu / mẫu URL | Quyền | JSON body | Kết quả thành công |
|---|---|---|---|---|
| POST | `/api/auth/register` | PUBLIC | A1 | 201 |
| POST | `/api/auth/login` | PUBLIC | A2 | 200 |
| GET | `/api/users/me` | Đăng nhập | — | 200 |
| POST | `/api/users/me/password` | Đăng nhập | A3 | 204 |
| GET | `/api/users/me/profile` | CUSTOMER | — | 200 |
| PUT | `/api/users/me/profile` | CUSTOMER | A4 | 200 |
| GET | `/api/admin/customers` | ADMIN | — | 200 |
| GET | `/api/admin/customers/{userId}/profile` | ADMIN | — | 200 |
| PATCH | `/api/admin/customers/{userId}/credit-limit` | ADMIN | A5 | 200 |
| GET | `/api/users/me/addresses` | CUSTOMER | — | 200 |
| POST | `/api/users/me/addresses` | CUSTOMER | A6 | 201 |
| PUT | `/api/users/me/addresses/{id}` | CUSTOMER | A6 | 200 |
| DELETE | `/api/users/me/addresses/{id}` | CUSTOMER | — | 204 |
| POST | `/api/admin/employee-accounts` | ADMIN | A7 | 201 |
| GET | `/api/admin/offices` | ADMIN | — | 200 |
| POST | `/api/admin/offices` | ADMIN | O1 | 201 |
| GET | `/api/admin/offices/{id}` | ADMIN | — | 200 |
| PUT | `/api/admin/offices/{id}` | ADMIN | O2 | 200 |
| GET | `/api/admin/employees` | ADMIN | — | 200 |
| POST | `/api/admin/employees` | ADMIN | O3 | 201 |
| GET | `/api/admin/employees/{userId}` | ADMIN | — | 200 |
| PUT | `/api/admin/employees/{userId}` | ADMIN | O4 | 200 |
| GET | `/api/products` | PUBLIC | — | 200 |
| GET | `/api/products/search?q=ban-phim&page=0&size=20` | PUBLIC | — | 200 |
| GET | `/api/products/{id}` | PUBLIC | — | 200 |
| POST | `/api/products` | ADMIN | P1 | 201 |
| PUT | `/api/products/{id}` | ADMIN | P2 | 200 |
| PATCH | `/api/products/{id}/stock` | ADMIN | P3 | 200 |
| DELETE | `/api/products/{id}` | ADMIN | — | 204 |
| GET | `/api/admin/products/{productId}/inventory` | ADMIN | — | 200 |
| POST | `/api/admin/products/{productId}/inventory/adjustments` | ADMIN | P4 | 201 |
| GET | `/api/categories` | PUBLIC | — | 200 |
| GET | `/api/categories/{id}` | PUBLIC | — | 200 |
| POST | `/api/admin/categories` | ADMIN | C1 | 201 |
| PUT | `/api/admin/categories/{id}` | ADMIN | C2 | 200 |
| GET | `/api/products/{productId}/categories` | PUBLIC | — | 200 |
| POST | `/api/admin/products/{productId}/categories/{categoryId}` | ADMIN | — | 204 |
| DELETE | `/api/admin/products/{productId}/categories/{categoryId}` | ADMIN | — | 204 |
| GET | `/api/suppliers` | ADMIN | — | 200 |
| GET | `/api/suppliers/{id}` | ADMIN | — | 200 |
| POST | `/api/admin/suppliers` | ADMIN | S1 | 201 |
| PUT | `/api/admin/suppliers/{id}` | ADMIN | S2 | 200 |
| GET | `/api/admin/products/{productId}/suppliers` | ADMIN | — | 200 |
| PUT | `/api/admin/products/{productId}/suppliers/{supplierId}` | ADMIN | S3 | 204 |
| DELETE | `/api/admin/products/{productId}/suppliers/{supplierId}` | ADMIN | — | 204 |
| GET | `/api/cart` | CUSTOMER | — | 200 |
| PUT | `/api/cart/items/{productId}` | CUSTOMER | K1 | 200 |
| DELETE | `/api/cart/items/{productId}` | CUSTOMER | — | 204 |
| DELETE | `/api/cart` | CUSTOMER | — | 204 |
| POST | `/api/cart/checkout` | CUSTOMER | K2 | 201 |
| POST | `/api/orders` | CUSTOMER | D1 | 201 |
| GET | `/api/orders/me` | CUSTOMER | — | 200 |
| GET | `/api/orders/me/search?code=&status=PENDING&page=0&size=20` | CUSTOMER | — | 200 |
| GET | `/api/orders/{id}` | Chủ đơn CUSTOMER | — | 200 |
| GET | `/api/orders/{orderId}/history` | Chủ đơn CUSTOMER | — | 200 |
| GET | `/api/orders/{orderId}/payments` | Chủ đơn CUSTOMER | — | 200 |
| GET | `/api/admin/orders` | ADMIN | — | 200 |
| GET | `/api/admin/orders/search?code=&status=PENDING&page=0&size=20` | ADMIN | — | 200 |
| PATCH | `/api/admin/orders/{id}/status` | ADMIN | D2 | 200 |
| GET | `/api/admin/orders/{orderId}/history` | ADMIN | — | 200 |
| GET | `/api/admin/orders/{orderId}/payments` | ADMIN | — | 200 |
| POST | `/api/admin/orders/{orderId}/payments` | ADMIN | T1 | 201 |
| PATCH | `/api/admin/payments/{paymentId}/status` | ADMIN | T2 | 200 |

## JSON body theo mã mẫu

Dùng từng object dưới đây trong ô **Request body** của endpoint có mã tương ứng. Một số endpoint POST/PUT dùng chung body, ví dụ địa chỉ A6. ID và mã duy nhất phải thay theo dữ liệu của bạn.

| Mã | JSON mẫu |
|---|---|
| A1 | `{"fullName":"Nguyễn Văn A","email":"customer-a@example.com","password":"Test@1234"}` |
| A2 | `{"email":"admin@example.com","password":"Admin@123"}` (H2 demo) |
| A3 | `{"currentPassword":"Strong@Pass123","newPassword":"New@Strong456"}` |
| A4 | `{"phone":"0901234567","dateOfBirth":"2000-01-01"}` |
| A5 | `{"creditLimit":1000000}` |
| A6 | `{"label":"Nhà","recipientName":"Nguyễn Văn A","recipientPhone":"0901234567","addressLine1":"1 Hà Nội","addressLine2":null,"ward":null,"district":null,"city":"Hà Nội","stateProvince":null,"postalCode":null,"countryCode":"VN","isDefault":true}` |
| A7 | `{"currentPassword":"Admin@123","fullName":"Nhân viên A","email":"staff-a@example.com","password":"Strong@Pass123","employeeCode":"EMP-A001","officeId":1,"managerUserId":null,"jobTitle":"Nhân viên bán hàng","extension":null,"hireDate":"2024-01-01"}` |
| O1 | `{"officeCode":"HN01","name":"Văn phòng Hà Nội","phone":"0901234567","email":"hn@example.com","addressLine1":"1 Tràng Tiền","addressLine2":null,"city":"Hà Nội","stateProvince":null,"postalCode":null,"countryCode":"VN"}` |
| O2 | `{"name":"Văn phòng Hà Nội mới","phone":"0901234567","email":"hn@example.com","addressLine1":"1 Tràng Tiền","addressLine2":null,"city":"Hà Nội","stateProvince":null,"postalCode":null,"countryCode":"VN","status":"ACTIVE"}` |
| O3 | `{"userId":1,"employeeCode":"EMP001","officeId":1,"managerUserId":null,"jobTitle":"Quản trị viên","extension":"101","hireDate":"2024-01-01"}` |
| O4 | `{"officeId":1,"managerUserId":null,"jobTitle":"Quản lý","extension":"102","hireDate":"2024-01-01","status":"ACTIVE"}` |
| P1 | `{"sku":"KB-001","name":"Bàn phím cơ","description":"Bàn phím mẫu","price":890000,"stockQuantity":20}` |
| P2 | `{"name":"Bàn phím cơ bản mới","description":"Mô tả mới","price":900000,"stockQuantity":20}` |
| P3 | `{"stockQuantity":15}` |
| P4 | `{"quantityChange":5,"note":"Nhập kho sau kiểm kê"}` (để xuất kho dùng số âm) |
| C1 | `{"code":"KEYBOARDS-TEST-01","name":"Bàn phím thử 01","description":"Thiết bị nhập liệu","parentId":null}` |
| C2 | `{"name":"Bàn phím mới","description":"Mô tả mới","parentId":null,"status":"ACTIVE"}` |
| S1 | `{"code":"SUP-001","name":"Nhà cung cấp A","contactName":"Nguyễn B","email":"supplier@example.com","phone":"0901234567"}` |
| S2 | `{"name":"Nhà cung cấp A mới","contactName":"Nguyễn B","email":"supplier@example.com","phone":"0901234567","status":"ACTIVE"}` |
| S3 | `{"supplierSku":"SKU-NCC-01","purchasePrice":500000,"leadTimeDays":3,"preferred":true}` |
| K1 | `{"quantity":2}` |
| K2 | `{"recipientName":"Nguyễn Văn A","recipientPhone":"0901234567","shippingAddress":"1 Hà Nội"}` |
| D1 | `{"recipientName":"Nguyễn Văn A","recipientPhone":"0901234567","shippingAddress":"1 Hà Nội","items":[{"productId":1,"quantity":2}]}` |
| D2 | `{"status":"CONFIRMED"}` (xem sơ đồ trạng thái trước khi đổi sang `SHIPPING`/`COMPLETED`/`CANCELLED`) |
| T1 | `{"method":"COD","amount":890000}` (`amount` không được vượt tổng đơn) |
| T2 | `{"status":"PAID","providerTransactionId":null}` (có thể đổi sang `FAILED`, `CANCELLED`, `REFUNDED` theo quy tắc thanh toán) |

Mã lỗi cần thử: `400` body/giá trị không hợp lệ; `401` thiếu hoặc sai token; `403` token đúng nhưng sai role; `404` ID không tồn tại hoặc đơn không thuộc customer; `409` trùng SKU, quá tồn kho hoặc chuyển trạng thái sai. Xem [thiết kế và trạng thái đơn](CORE_DESIGN.md) và [test tự động](../src/test/java/vn/edu/sales/SalesApiIntegrationTest.java).
