# TenantCore + LogiFlow - FE API Integration Guide (Full Field Contract)

**Mục tiêu:** contract dùng trực tiếp cho FE mapper, gồm **header cần truyền**, API, request/response và toàn bộ field theo DTO backend hiện tại.

---

## 1) Base URL + Header contract

- **Gateway base URL:** `http://localhost:8080`
- **Header dùng trong hệ thống:**
  - `Authorization: Bearer {accessToken}`
  - `X-Tenant-Code: {tenantCode}`
  - `Content-Type: application/json` (POST/PUT/PATCH)

### 1.1 Quy tắc header theo nhóm endpoint

| Nhóm endpoint | Authorization | X-Tenant-Code |
|---|---|---|
| `/api/core/health`, `/api/logiflow/health` | Không cần | Không cần |
| `/api/auth/setup-password`, `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout` | Không cần | Nên gửi (backend chấp nhận thiếu và dùng mặc định/tenant theo token ở một số flow) |
| `/api/auth/me` | Bắt buộc | Nên gửi (nếu gửi phải khớp tenant trong token) |
| Toàn bộ `/api/logiflow/**` nghiệp vụ | Bắt buộc | Nên gửi (nếu thiếu backend có thể suy ra từ token; nếu sai sẽ bị forbidden) |

---

## 2) Envelope chuẩn cho FE mapper

## 2.1 Success envelope (`ApiResponse<T>`)

```json
{
  "code": "SUCCESS",
  "message": "Success",
  "data": {},
  "timestamp": "2026-05-15T10:00:00"
}
```

Field:
- `code: string`
- `message: string`
- `data: T`
- `timestamp: string (LocalDateTime)`

## 2.2 Error envelope (`ErrorResponse`)

```json
{
  "code": "TENANT_FORBIDDEN",
  "message": "Tenant code is invalid or not allowed",
  "details": null,
  "path": "/api/logiflow/orders",
  "timestamp": "2026-05-15T10:00:00"
}
```

Field:
- `code: string`
- `message: string`
- `details: object | null`
- `path: string`
- `timestamp: string (LocalDateTime)`

## 2.3 Pagination envelope (`PageResponse<T>`)

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

Field:
- `items: T[]`
- `page: number`
- `size: number`
- `totalElements: number`
- `totalPages: number`
- `first: boolean`
- `last: boolean`

---

## 3) Auth APIs

## 3.1 `POST /api/auth/setup-password`

Headers:
- `X-Tenant-Code` (recommended)
- `Content-Type: application/json`

Request model: `SetupPasswordRequest`
- `username: string` (required)
- `password: string` (required)

Response model: `ApiResponse<string>`
- `data: "OK"`

## 3.2 `POST /api/auth/login`

Headers:
- `X-Tenant-Code` (recommended)
- `Content-Type: application/json`

Request model: `LoginRequest`
- `username: string` (required)
- `password: string` (required)

Response model: `ApiResponse<LoginResponse>`

`LoginResponse` fields:
- `accessToken: string`
- `refreshToken: string`
- `tokenType: string`
- `expiresIn: number`
- `user: CurrentUserResponse`

`CurrentUserResponse` fields:
- `userId: string (UUID)`
- `tenantCode: string`
- `username: string`
- `fullName: string`
- `roles: string[]`
- `permissions: string[]`

## 3.3 `GET /api/auth/me`

Headers:
- `Authorization` (**required**)
- `X-Tenant-Code` (recommended)

Response model: `ApiResponse<CurrentUserResponse>`

## 3.4 `POST /api/auth/refresh`

Headers:
- `X-Tenant-Code` (recommended)
- `Content-Type: application/json`

Request model: `RefreshRequest`
- `refreshToken: string` (required)

Response model: `ApiResponse<TokenPairResponse>`

`TokenPairResponse` fields:
- `accessToken: string`
- `refreshToken: string`
- `tokenType: string`
- `expiresIn: number`

## 3.5 `POST /api/auth/logout`

Headers:
- `X-Tenant-Code` (recommended)
- `Content-Type: application/json`

Request model: `RefreshRequest`
- `refreshToken: string` (required)

Response model: `ApiResponse<string>`
- `data: "OK"`

---

## 4) LogiFlow APIs

> Tất cả endpoint dưới đây dùng:
> - `Authorization: Bearer {accessToken}`
> - `X-Tenant-Code: {tenantCode}`

## 4.1 Orders

## `POST /api/logiflow/orders`
Request model: `CreateOrderRequest`
- `receiverName: string` (required)
- `receiverAddress: string` (required)
- `codAmount: number` (BigDecimal, required)

Response model: `ApiResponse<OrderResponse>`

## `GET /api/logiflow/orders/{id}`
Response model: `ApiResponse<OrderResponse>`

## `GET /api/logiflow/orders?page=&size=&status=&keyword=`
Query params:
- `page: number` (default `0`)
- `size: number` (default `20`)
- `status: string` (optional)
- `keyword: string` (optional)

Response model: `ApiResponse<PageResponse<OrderResponse>>`

## `PATCH /api/logiflow/orders/{id}/status`
Request model: `UpdateOrderStatusRequest`
- `status: string` (required)
- `reason: string` (optional)

Response model: `ApiResponse<OrderResponse>`

## `POST /api/logiflow/orders/{id}/assign`
Request model: `AssignOrderRequest`
- `driverId: string (UUID)` (optional)
- `vehicleId: string (UUID)` (optional)
- `note: string` (optional)

Response model: `ApiResponse<SimpleActionResponse>`

## `POST /api/logiflow/orders/{id}/tracking`
Request model: `TrackingEventRequest`
- `eventCode: string` (required)
- `eventName: string` (required)
- `description: string` (optional)
- `locationText: string` (optional)
- `latitude: number` (BigDecimal, optional)
- `longitude: number` (BigDecimal, optional)
- `eventTime: string` (LocalDateTime, optional)

Response model: `ApiResponse<SimpleActionResponse>`

## `POST /api/logiflow/orders/{id}/cod`
Request model: `UpdateCodRequest`
- `amount: number` (BigDecimal, required)
- `status: string` (optional)
- `note: string` (optional)

Response model: `ApiResponse<SimpleActionResponse>`

## `OrderResponse` full fields
- `id: string (UUID)`
- `tenantCode: string`
- `orderCode: string`
- `receiverName: string`
- `receiverAddress: string`
- `codAmount: number`
- `status: string`
- `createdAt: string (LocalDateTime)`

## `SimpleActionResponse` full fields
- `action: string`
- `status: string`

---

## 4.2 Customers

## `POST /api/logiflow/customers`
Request model: `CreateCustomerRequest`
- `customerCode: string` (required)
- `customerName: string` (required)
- `phone: string` (optional)
- `email: string` (optional)
- `address: string` (optional)
- `type: string` (optional)

Response model: `ApiResponse<CustomerResponse>`

## `GET /api/logiflow/customers/{id}`
Response model: `ApiResponse<CustomerResponse>`

## `GET /api/logiflow/customers?page=&size=&status=&keyword=`
Query params:
- `page: number` (default `0`)
- `size: number` (default `20`)
- `status: string` (optional)
- `keyword: string` (optional)

Response model: `ApiResponse<PageResponse<CustomerResponse>>`

## `PUT /api/logiflow/customers/{id}`
Request model: `UpdateCustomerRequest`
- `customerName: string` (required)
- `phone: string` (optional)
- `email: string` (optional)
- `address: string` (optional)
- `type: string` (optional)
- `status: string` (optional)

Response model: `ApiResponse<CustomerResponse>`

## `DELETE /api/logiflow/customers/{id}`
Response model: `ApiResponse<SimpleActionResponse>`

## `CustomerResponse` full fields
- `id: string (UUID)`
- `tenantCode: string`
- `customerCode: string`
- `customerName: string`
- `phone: string`
- `email: string`
- `address: string`
- `type: string`
- `status: string`
- `createdAt: string (LocalDateTime)`

---

## 4.3 Drivers

## `POST /api/logiflow/drivers`
Request model: `CreateDriverRequest`
- `driverCode: string` (required)
- `fullName: string` (required)
- `phone: string` (optional)
- `email: string` (optional)
- `licenseNumber: string` (optional)

Response model: `ApiResponse<DriverResponse>`

## `GET /api/logiflow/drivers/{id}`
Response model: `ApiResponse<DriverResponse>`

## `GET /api/logiflow/drivers?page=&size=&status=&keyword=`
Query params:
- `page: number` (default `0`)
- `size: number` (default `20`)
- `status: string` (optional)
- `keyword: string` (optional)

Response model: `ApiResponse<PageResponse<DriverResponse>>`

## `PUT /api/logiflow/drivers/{id}`
Request model: `UpdateDriverRequest`
- `fullName: string` (required)
- `phone: string` (optional)
- `email: string` (optional)
- `licenseNumber: string` (optional)
- `status: string` (optional)

Response model: `ApiResponse<DriverResponse>`

## `DELETE /api/logiflow/drivers/{id}`
Response model: `ApiResponse<SimpleActionResponse>`

## `DriverResponse` full fields
- `id: string (UUID)`
- `tenantCode: string`
- `driverCode: string`
- `fullName: string`
- `phone: string`
- `email: string`
- `licenseNumber: string`
- `status: string`
- `createdAt: string (LocalDateTime)`

---

## 4.4 Vehicles

## `POST /api/logiflow/vehicles`
Request model: `CreateVehicleRequest`
- `vehicleCode: string` (required)
- `plateNumber: string` (required)
- `vehicleType: string` (optional)
- `capacityKg: number` (BigDecimal, optional)

Response model: `ApiResponse<VehicleResponse>`

## `GET /api/logiflow/vehicles/{id}`
Response model: `ApiResponse<VehicleResponse>`

## `GET /api/logiflow/vehicles?page=&size=&status=&keyword=`
Query params:
- `page: number` (default `0`)
- `size: number` (default `20`)
- `status: string` (optional)
- `keyword: string` (optional)

Response model: `ApiResponse<PageResponse<VehicleResponse>>`

## `PUT /api/logiflow/vehicles/{id}`
Request model: `UpdateVehicleRequest`
- `plateNumber: string` (required)
- `vehicleType: string` (optional)
- `capacityKg: number` (BigDecimal, optional)
- `status: string` (optional)

Response model: `ApiResponse<VehicleResponse>`

## `DELETE /api/logiflow/vehicles/{id}`
Response model: `ApiResponse<SimpleActionResponse>`

## `VehicleResponse` full fields
- `id: string (UUID)`
- `tenantCode: string`
- `vehicleCode: string`
- `plateNumber: string`
- `vehicleType: string`
- `capacityKg: number`
- `status: string`
- `createdAt: string (LocalDateTime)`

---

## 4.5 Reconciliation

## `POST /api/logiflow/reconciliations`
Request model: `CreateReconciliationRequest`
- `driverId: string (UUID)` (optional)
- `codRecordIds: string[] (UUID[])` (required, non-empty)
- `note: string` (optional)

Response model: `ApiResponse<ReconciliationResponse>`

## `GET /api/logiflow/reconciliations/eligible-cod?page=&size=&keyword=`
Query params:
- `page: number` (default `0`)
- `size: number` (default `20`)
- `keyword: string` (optional)

Response model: `ApiResponse<PageResponse<EligibleCodRecordResponse>>`

## `GET /api/logiflow/reconciliations?page=&size=&status=&keyword=`
Query params:
- `page: number` (default `0`)
- `size: number` (default `20`)
- `status: string` (optional)
- `keyword: string` (optional)

Response model: `ApiResponse<PageResponse<ReconciliationResponse>>`

## `GET /api/logiflow/reconciliations/{id}`
Response model: `ApiResponse<ReconciliationResponse>`

## `PATCH /api/logiflow/reconciliations/{id}/status`
Request model: `UpdateReconciliationStatusRequest`
- `status: string` (required)
- `note: string` (optional)

Response model: `ApiResponse<ReconciliationResponse>`

## `EligibleCodRecordResponse` full fields
- `id: string (UUID)`
- `orderId: string (UUID)`
- `amount: number` (BigDecimal)
- `status: string`
- `createdAt: string (LocalDateTime)`

## `ReconciliationResponse` full fields
- `id: string (UUID)`
- `tenantCode: string`
- `reconciliationCode: string`
- `driverId: string (UUID) | null`
- `totalOrders: number`
- `totalCodAmount: number` (BigDecimal)
- `status: string`
- `reconciledAt: string (LocalDateTime) | null`
- `note: string | null`
- `createdAt: string (LocalDateTime)`

---

## 4.6 Operations / Reports

## `GET /api/logiflow/operations/cod/summary`
Response model: `ApiResponse<CodSummaryResponse>`

`CodSummaryResponse` fields:
- `totalRecords: number`
- `totalAmount: number` (BigDecimal)
- `pendingAmount: number` (BigDecimal)
- `collectedAmount: number` (BigDecimal)
- `reconciledAmount: number` (BigDecimal)

## `GET /api/logiflow/operations/cod/daily?fromDate=&toDate=`
Query params:
- `fromDate: string (yyyy-MM-dd)` (optional)
- `toDate: string (yyyy-MM-dd)` (optional)

Response model: `ApiResponse<CodDailyReportItem[]>`

`CodDailyReportItem` fields:
- `businessDate: string (LocalDate)`
- `totalRecords: number`
- `totalAmount: number` (BigDecimal)
- `pendingAmount: number` (BigDecimal)
- `collectedAmount: number` (BigDecimal)
- `reconciledAmount: number` (BigDecimal)

## `GET /api/logiflow/operations/reconciliation/by-driver?fromDate=&toDate=`
Query params:
- `fromDate: string (yyyy-MM-dd)` (optional)
- `toDate: string (yyyy-MM-dd)` (optional)

Response model: `ApiResponse<ReconciliationDriverReportItem[]>`

`ReconciliationDriverReportItem` fields:
- `driverId: string (UUID)`
- `totalReconciliations: number`
- `totalOrders: number`
- `totalCodAmount: number` (BigDecimal)
- `reconciledCount: number`

---

## 5) FE TypeScript mapping đề xuất

```ts
export type ApiResponse<T> = {
  code: string;
  message: string;
  data: T;
  timestamp: string;
};

export type ApiError = {
  code: string;
  message: string;
  details: Record<string, unknown> | null;
  path: string;
  timestamp: string;
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};
```

---

## 6) Nguồn chuẩn để đồng bộ khi backend đổi

- Postman: `postman/tenantcore-gateway-mvp.postman_collection.json`
- Swagger/OpenAPI:
  - `http://localhost:8081/swagger-ui/index.html`
  - `http://localhost:8082/swagger-ui/index.html`
  - `http://localhost:8082/v3/api-docs`

Nếu có chênh lệch, FE ưu tiên runtime/OpenAPI mới nhất.
