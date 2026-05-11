# TenantCore + LogiFlow MVP - Guide Rút Gọn (Unified Flow)

## 1) Mục tiêu & phạm vi
- Duy trì kiến trúc hiện tại, không tách service thêm ở MVP.
- Luồng thống nhất: `gateway-service (8080)` -> `core-service (8081)` / `logiflow-service (8082)` -> Neon PostgreSQL.
- Multi-tenant bắt buộc: mọi bảng nghiệp vụ có `tenant_code`, mọi query nghiệp vụ phải filter `tenant_code`.

## 2) Kiến trúc module
- `common-lib`: DTO, exception, context, constants dùng chung.
- `core-service` (Spring MVC): IAM + Flyway migration.
- `logiflow-service` (Spring MVC): nghiệp vụ logistics, dùng DB, tắt Flyway.
- `gateway-service` (Spring Cloud Gateway WebFlux): route + security/filter gateway.

## 3) Chuẩn kỹ thuật
- Java 21, Maven multi-module, Spring Boot 3.5.x, Spring Cloud 2025.0.x.
- PostgreSQL (Neon), JPA, Flyway (chỉ `core-service`).

## 4) Quy tắc config quan trọng
- Không hard-code secret trong production.
- Hiện tại test đang gắn trực tiếp DB trong:
  - `core-service/src/main/resources/application.yaml`
  - `logiflow-service/src/main/resources/application.yaml`
- Khi chuyển môi trường chuẩn: đổi lại dùng env var `NEON_DB_PASSWORD`.

## 5) Trạng thái đã hoàn thành (tính đến 2026-05-11)
- [x] Stage 1: Chuẩn hóa Maven multi-module.
- [x] Stage 2: Hoàn thiện `common-lib` (DTO/exception/context/security).
- [x] Stage 3: Bootstrapping service + health API + gateway routes.
- [x] Stage 4: Kết nối Neon config cho core/logiflow.
- [x] Stage 5: Flyway runtime verify thành công (`schema version 003`, health core OK).
- [x] Stage 6: Mock API:
  - `POST /api/auth/login`
  - `GET /api/auth/me`
  - `POST /api/logiflow/orders`
  - `GET /api/logiflow/orders/{id}`
- [~] Stage 7 (đang làm):
  - [x] Đã tạo `BaseEntity`
  - [x] Đã tạo IAM entities/repositories trong `core-service`:
    - `Tenant`, `User`, `Role`, `Permission`, `RefreshToken`
    - bảng liên kết `user_roles`, `role_permissions`
  - [x] Đã chuyển `/api/auth/login` và `/api/auth/me` sang logic thật (BCrypt + JWT + DB lookup)
  - [x] Đã thêm migration `V004__seed_demo_admin_user.sql` để seed user demo + OWNER role mapping
  - [x] Đã khôi phục `GlobalExceptionHandler` trong `common-lib` để trả lỗi chuẩn `ErrorResponse`
  - [x] Đã thêm setup API `POST /api/auth/setup-password` để set BCrypt password cho user demo khi cần
  - [x] Đã verify end-to-end (core-service trực tiếp):
    - `POST /api/auth/setup-password` -> SUCCESS
    - `POST /api/auth/login` -> SUCCESS (trả accessToken/refreshToken)
    - `GET /api/auth/me` với Bearer token -> SUCCESS
  - [x] Đã thêm auth API thật:
    - `POST /api/auth/refresh`
    - `POST /api/auth/logout`
  - [x] Đã verify flow auth đầy đủ trên core-service:
    - `login` SUCCESS
    - `refresh` SUCCESS
    - `me` SUCCESS
    - `logout` SUCCESS
    - `refresh` sau `logout` trả 401 (đúng kỳ vọng)

## 6) Luồng chạy local thống nhất
1. Start `core-service` (8081).
2. Start `logiflow-service` (8082).
3. Start `gateway-service` (8080).
4. Test qua gateway trước:
   - `GET /api/core/health`
   - `GET /api/logiflow/health`
   - `POST /api/auth/login`
   - `GET /api/auth/me`
   - `POST /api/logiflow/orders`
   - `GET /api/logiflow/orders/{id}`

Ghi chú thực tế:
- `gateway-service` cần `SecurityWebFilterChain` permit `"/api/**"` trong giai đoạn MVP, nếu không gateway sẽ trả `401` trước khi route.

Postman qua gateway:
- Collection: `postman/tenantcore-gateway-mvp.postman_collection.json`
- Biến chính:
  - `baseUrl = http://localhost:8080`
  - `tenantCode = demo`
  - `accessToken`, `refreshToken`, `orderId` được set tự động sau request login/create-order

Kết quả verify qua gateway (`:8080`) đã pass:
- `GET /api/core/health` -> SUCCESS
- `GET /api/logiflow/health` -> SUCCESS
- `POST /api/auth/setup-password` -> SUCCESS
- `POST /api/auth/login` -> SUCCESS
- `GET /api/auth/me` -> SUCCESS
- `POST /api/auth/refresh` -> SUCCESS
- `POST /api/logiflow/orders` -> SUCCESS
- `GET /api/logiflow/orders/{id}` -> SUCCESS
- `POST /api/auth/logout` -> SUCCESS
- `POST /api/auth/refresh` sau logout -> 401 (expected)

## 7) Nguyên tắc migration
- Migration nằm ở `core-service/src/main/resources/db/migration`.
- Không sửa migration đã chạy trên DB có dữ liệu thật.
- Cần thay đổi schema: tạo version migration mới.

## 8) Quy tắc coding trước Stage 7
- Ưu tiên reuse class từ `common-lib` cho response/error.
- API trả theo `ApiResponse` / `ErrorResponse`.
- Lỗi nghiệp vụ dùng `BusinessException` + `ErrorCode`.
- Không mở rộng phạm vi ngoài checklist Stage 7.

## 9) Kế hoạch thực thi Stage 7 (thứ tự bắt buộc)
1. Tạo `BaseEntity` + chuẩn hóa cột audit.
2. Chuẩn hóa tenant mapping (`tenant_code`) cho entity nghiệp vụ.
3. Tạo entity/repository nhóm IAM trong `core-service`:
   - `Tenant`, `User`, `Role`, `Permission`, `RefreshToken`.
4. Làm auth thật:
   - BCrypt password
   - JWT service
   - login / refresh / logout / me
5. Chuyển dần mock API sang service thật, vẫn giữ contract endpoint hiện tại.
6. Sau mỗi nhóm thay đổi: `mvn clean install -DskipTests`.

Tiến độ Stage 7 mới nhất:
- [x] Đã chuyển `logiflow orders` từ mock sang DB-backed trong `logiflow-service`:
  - entity: `logiflow_orders`
  - repository: query theo `(id, tenant_code)`
  - service: create/get theo tenant
  - controller giữ nguyên endpoint contract cũ
- [x] Đã verify qua gateway:
  - `POST /api/logiflow/orders` -> SUCCESS
  - `GET /api/logiflow/orders/{id}` -> SUCCESS
  - dữ liệu `id/orderCode/tenantCode` khớp giữa create và get
- [x] Đã refactor tách lớp rõ ràng theo module:
  - `core-service/auth`: `api` + `web (controller impl)` + `application (facade)` + `service`
  - `logiflow-service/order`: `api` + `web (controller impl)` + `application (facade)` + `service`
- [x] Smoke test sau refactor qua gateway:
  - `setup-password`, `login`, `me`, `create order`, `get order` đều SUCCESS
- [x] Đã mở rộng order use-case thật:
  - `GET /api/logiflow/orders?page=&size=&status=&keyword=`
  - phân trang bằng `PageResponse`, query luôn filter theo `tenant_code`
  - verify qua gateway: list all + keyword filter đều SUCCESS
- [~] Đang bổ sung order status workflow:
  - `PATCH /api/logiflow/orders/{id}/status`
  - cập nhật trạng thái tenant-aware (NEW/ASSIGNED/IN_TRANSIT/COMPLETED/CANCELLED)
- [x] Đã verify order status workflow qua gateway:
  - create order -> update status COMPLETED -> get order trả status COMPLETED
  - list filter theo `status=COMPLETED` + `keyword` trả kết quả đúng
- [x] Đã bổ sung JWT auth filter ở `core-service`:
  - parse Bearer token thành security context
  - security stateless + authenticated cho endpoint không-public
  - verify qua gateway:
    - `/api/auth/me` có token -> SUCCESS
    - `/api/auth/me` không token -> 401 (expected)
- [~] Đang bổ sung security enforcement cho `logiflow-service`:
  - JWT filter parse token + set security context
  - enforce tenant header/token mismatch -> 403
  - enforce permission theo endpoint order (VIEW/CREATE/UPDATE)
- [x] Đã verify security enforcement qua gateway:
  - gọi order API không token -> 401 (expected)
  - token hợp lệ nhưng `X-Tenant-Code` sai -> 403 (expected)
  - token + tenant đúng -> create/list order SUCCESS
- [x] Đã triển khai logistics workflow tiếp theo:
  - `POST /api/logiflow/orders/{id}/assign`
  - `POST /api/logiflow/orders/{id}/tracking`
  - `POST /api/logiflow/orders/{id}/cod`
  - verify qua gateway: create -> assign -> tracking -> cod đều SUCCESS, và no-token assign trả 401

## 10) Definition of Done cho Stage 7
- Build pass toàn bộ project.
- Auth flow thật chạy qua gateway.
- Query nghiệp vụ luôn có tenant filter.
- Không lộ password/secret trong source commit.
- Guide trạng thái được cập nhật lại sau mỗi mốc hoàn thành.
