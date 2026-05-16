# TenantCore + LogiFlow MVP - Business Features Outline

**Last Updated**: 2026-05-14  
**Status**: Stage 7 Complete (Ready for Stage 8)

---

## 📱 Quick Overview

The platform is a **multi-tenant logistics management system** with:
- **Core Service**: User authentication, roles, permissions, tenant management
- **LogiFlow Service**: Order management, driver/vehicle management, COD reconciliation
- **Gateway Service**: API routing and security enforcement

All data is **strictly tenant-isolated** - no cross-tenant data leakage.

---

## 🔐 Core Service (Authentication & User Management)

### What It Does
Manages user authentication, tenant isolation, and permission enforcement.

### API Endpoints

| Feature | Endpoint | Method | Description |
|---------|----------|--------|-------------|
| **Login** | `/api/auth/login` | `POST` | Authenticate user with username/email/phone + password + tenant code |
| **My Profile** | `/api/auth/me` | `GET` | Get current logged-in user info + their roles/permissions |
| **Setup Password** | `/api/auth/setup-password` | `POST` | Create/reset user password |
| **Refresh Token** | `/api/auth/refresh` | `POST` | Get new access token using refresh token |
| **Logout** | `/api/auth/logout` | `POST` | Revoke refresh token and logout |

### Business Entities
- **Users** - Tenant users with username, email, phone, full name
- **Tenants** - Company accounts with tenant code, domain, billing plan
- **Roles** - RBAC roles (OWNER, ADMIN, MEMBER) per tenant
- **Permissions** - Fine-grained permissions (e.g., `LOGIFLOW_ORDER_VIEW`, `LOGIFLOW_CUSTOMER_CREATE`)
- **User-Role Mapping** - Assign roles to users
- **Refresh Tokens** - Session management with device tracking

### Key Business Logic
1. **Login Flow**: Accept username/email/phone → Validate credentials → Check tenant → Resolve user permissions → Return JWT + Refresh Token
2. **Flexible Identifier**: Login supports username, email, or phone (case-insensitive for username/email)
3. **Token Refresh**: Validate refresh token → Issue new access token (without re-authenticating)
4. **Logout**: Revoke refresh token (prevent reuse)
5. **Current scope decision**: Keep **1 user = 1 tenant** for this phase. Auth/authorization resolves tenant from `users.tenant_code` + JWT tenant claim.
6. **Future scale-up note**: `user_tenants` is reserved for multi-tenant membership (one user in multiple tenants) and is not active in current auth flow.

### Database Tables
```
tenants              -- Tenant master data
users                -- Users per tenant
roles                -- Roles per tenant
permissions          -- Global permissions
user_roles           -- User ↔ Role mapping
role_permissions     -- Role ↔ Permission mapping
refresh_tokens       -- Session tokens (with device tracking)
user_tenants         -- Reserved for future multi-tenant user membership (not used in current phase)
```

---

## 📦 LogiFlow Service (Logistics & Order Management)

### What It Does
Manages the core logistics workflow: orders, drivers, vehicles, customers, COD collection, and settlement.

---

### 1️⃣ Order Management

#### Endpoints
| Feature | Endpoint | Method | Permission | Description |
|---------|----------|--------|-----------|-------------|
| Create Order | `/api/logiflow/orders` | `POST` | `LOGIFLOW_ORDER_CREATE` | Create new delivery order |
| Get Order | `/api/logiflow/orders/{id}` | `GET` | `LOGIFLOW_ORDER_VIEW` | Get order details |
| List Orders | `/api/logiflow/orders` | `GET` | `LOGIFLOW_ORDER_VIEW` | List orders (paginated, filterable by status/keyword) |
| Update Status | `/api/logiflow/orders/{id}/status` | `PATCH` | `LOGIFLOW_ORDER_UPDATE` | Change order status (NEW → IN_TRANSIT → COMPLETED/CANCELLED) |
| Assign Driver | `/api/logiflow/orders/{id}/assign` | `POST` | `LOGIFLOW_ORDER_ASSIGN` | Assign order to driver + vehicle |
| Add Tracking | `/api/logiflow/orders/{id}/tracking` | `POST` | `LOGIFLOW_ORDER_TRACKING` | Record delivery location/event (GPS tracking) |
| Update COD | `/api/logiflow/orders/{id}/cod` | `POST` | `LOGIFLOW_COD_UPDATE` | Update cash-on-delivery amount |

#### Order Status Flow
```
NEW (created)
  ↓
IN_TRANSIT (driver assigned, moving)
  ↓
COMPLETED (delivered) or CANCELLED
```

#### Database Entities
- **Orders** - Core order data (sender, receiver, COD amount, status)
- **Delivery Assignments** - Links order to driver + vehicle + status (pending/accepted/in-transit/completed)
- **Tracking Events** - Order tracking history (location, time, event type)
- **COD Records** - Cash-on-delivery payment tracking (PENDING → COLLECTED → RECONCILED)

---

### 2️⃣ Driver Management

#### Endpoints
| Feature | Endpoint | Method | Permission | Description |
|---------|----------|--------|-----------|-------------|
| Create Driver | `/api/logiflow/drivers` | `POST` | `LOGIFLOW_DRIVER_CREATE` | Register new driver + create user account for login |
| Get Driver | `/api/logiflow/drivers/{id}` | `GET` | `LOGIFLOW_DRIVER_VIEW` | Get driver profile |
| List Drivers | `/api/logiflow/drivers` | `GET` | `LOGIFLOW_DRIVER_VIEW` | List drivers (paginated, filter by status/name) |
| Update Driver | `/api/logiflow/drivers/{id}` | `PUT` | `LOGIFLOW_DRIVER_UPDATE` | Update driver info (phone, license, etc.) |
| Delete Driver | `/api/logiflow/drivers/{id}` | `DELETE` | `LOGIFLOW_DRIVER_DELETE` | Soft-delete driver (mark inactive) |

#### Driver Authentication
- **Driver Login**: Use `/api/auth/login` with driver's username/password to get JWT token
- **Driver Profile Link**: Each driver has `user_id` FK linking to user account for authentication
- **Driver Web App**: Dedicated web app for drivers to view assigned orders + update tracking status (filtered by current user's driver profile)
- **Role & Permissions**: Drivers assigned MEMBER role with `LOGIFLOW_ORDER_VIEW`, `LOGIFLOW_ORDER_UPDATE`, `LOGIFLOW_ORDER_TRACKING` permissions

#### Driver Data
- Driver code, full name, phone, email
- License number, status (ACTIVE/INACTIVE)
- Linked user account (user_id) for authentication
- Assigned vehicles and current orders

---

### 3️⃣ Vehicle Management

#### Endpoints
| Feature | Endpoint | Method | Permission | Description |
|---------|----------|--------|-----------|-------------|
| Create Vehicle | `/api/logiflow/vehicles` | `POST` | `LOGIFLOW_VEHICLE_CREATE` | Register new vehicle |
| Get Vehicle | `/api/logiflow/vehicles/{id}` | `GET` | `LOGIFLOW_VEHICLE_VIEW` | Get vehicle details |
| List Vehicles | `/api/logiflow/vehicles` | `GET` | `LOGIFLOW_VEHICLE_VIEW` | List vehicles (paginated, filter) |
| Update Vehicle | `/api/logiflow/vehicles/{id}` | `PUT` | `LOGIFLOW_VEHICLE_UPDATE` | Update vehicle info (plate, capacity, etc.) |
| Delete Vehicle | `/api/logiflow/vehicles/{id}` | `DELETE` | `LOGIFLOW_VEHICLE_DELETE` | Soft-delete vehicle |

#### Vehicle Data
- Vehicle code, plate number, vehicle type (truck, van, motorcycle)
- Capacity (kg), status (ACTIVE/INACTIVE)
- Assigned driver and current orders

---

### 4️⃣ Customer Management

#### Endpoints
| Feature | Endpoint | Method | Permission | Description |
|---------|----------|--------|-----------|-------------|
| Create Customer | `/api/logiflow/customers` | `POST` | `LOGIFLOW_CUSTOMER_CREATE` | Register new customer |
| Get Customer | `/api/logiflow/customers/{id}` | `GET` | `LOGIFLOW_CUSTOMER_VIEW` | Get customer profile |
| List Customers | `/api/logiflow/customers` | `GET` | `LOGIFLOW_CUSTOMER_VIEW` | List customers (paginated, searchable) |
| Update Customer | `/api/logiflow/customers/{id}` | `PUT` | `LOGIFLOW_CUSTOMER_UPDATE` | Update customer info |
| Delete Customer | `/api/logiflow/customers/{id}` | `DELETE` | `LOGIFLOW_CUSTOMER_DELETE` | Soft-delete customer |

#### Customer Data
- Customer code, name, phone, email, address
- Type (individual/business), status (ACTIVE/INACTIVE)
- Order history

---

### 5️⃣ COD (Cash-on-Delivery) & Reconciliation

#### What Is COD?
Orders can have a **Cash-on-Delivery** amount - the driver collects cash from receiver and settles with company later.

#### COD Payment Lifecycle
```
NEW ORDER (with COD amount)
  ↓
COD RECORD: PENDING (waiting for collection)
  ↓
DRIVER COLLECTS CASH (updates order via /api/logiflow/orders/{id}/cod)
  ↓
COD RECORD: COLLECTED (ready for settlement)
  ↓
CREATE RECONCILIATION BATCH (group collected CODs)
  ↓
RECONCILIATION: RECONCILED (finalized, money transferred)
```

#### Reconciliation Endpoints

| Feature | Endpoint | Method | Permission | Description |
|---------|----------|--------|-----------|-------------|
| List Eligible COD | `/api/logiflow/reconciliations/eligible-cod` | `GET` | `LOGIFLOW_RECONCILIATION_VIEW` | Show COD records available for settlement (COLLECTED status, not linked) |
| Create Reconciliation | `/api/logiflow/reconciliations` | `POST` | `LOGIFLOW_RECONCILIATION_CREATE` | Create settlement batch from selected COD records |
| Get Reconciliation | `/api/logiflow/reconciliations/{id}` | `GET` | `LOGIFLOW_RECONCILIATION_VIEW` | Get reconciliation batch details |
| List Reconciliations | `/api/logiflow/reconciliations` | `GET` | `LOGIFLOW_RECONCILIATION_VIEW` | List reconciliations (paginated, filter by status) |
| Update Status | `/api/logiflow/reconciliations/{id}/status` | `PATCH` | `LOGIFLOW_RECONCILIATION_UPDATE` | Change status (OPEN → RECONCILED → CLOSED) |

#### Reconciliation Status Flow
```
OPEN (batch created, pending approval)
  ↓
RECONCILED (settled, money transferred)
  ↓
CLOSED (finalized)
```

#### Database Entities
- **COD Records** - Payment tracking per order (amount, status: PENDING/COLLECTED/RECONCILED)
- **Reconciliations** - Settlement batches (date, driver, total amount, status)
- **FK Relationship**: COD Records → Reconciliations (one reconciliation can have many COD records)

#### Reconciliation Business Rules
- Only **COLLECTED** COD records (not yet reconciled) can be included
- Optional: Filter by driver (must be ACTIVE)
- Optional: Filter by time window (max COD age in hours, configurable)
- When reconciliation is updated to **RECONCILED**, all linked COD records are marked RECONCILED

---

### 6️⃣ Operations & Reporting

#### Endpoints

| Feature | Endpoint | Method | Permission | Description |
|---------|----------|--------|-----------|-------------|
| COD Summary | `/api/logiflow/operations/cod/summary` | `GET` | `LOGIFLOW_COD_VIEW` | Overview: total/pending/collected/reconciled COD amounts |
| Daily COD Report | `/api/logiflow/operations/cod/daily` | `GET` | `LOGIFLOW_COD_VIEW` | COD aggregates by date (last 7 days by default, max 31 days) |
| Driver Reconciliation | `/api/logiflow/operations/reconciliation/by-driver` | `GET` | `LOGIFLOW_RECONCILIATION_VIEW` | Settlement totals per driver (date range aggregates) |

#### Example Responses

**COD Summary**:
```json
{
  "totalAmount": 50000000,
  "pendingAmount": 5000000,
  "collectedAmount": 25000000,
  "reconciledAmount": 20000000
}
```

**Daily COD Report** (last 7 days):
```json
{
  "data": [
    {"date": "2026-05-14", "collectedAmount": 3500000, "recordCount": 8},
    {"date": "2026-05-13", "collectedAmount": 4200000, "recordCount": 10},
    ...
  ]
}
```

**Driver Reconciliation**:
```json
{
  "data": [
    {"driverId": "driver-001", "driverName": "Nguyễn Văn A", "totalAmount": 5000000, "recordCount": 12},
    ...
  ]
}
```

---

## 🔒 Security & Multi-Tenancy

### How It Works
1. **Login** → User provides `X-Tenant-Code` header + credentials
2. **JWT Token** → Server returns access token containing:
   - `userId` - Who is this user
   - `tenantCode` - Which tenant
   - `roles` - User's roles
   - `permissions` - Fine-grained permissions
3. **Subsequent Requests** → Client includes:
   - `Authorization: Bearer {accessToken}` header
   - `X-Tenant-Code` header
4. **Validation** → Server checks:
   - Token is valid (not expired, signature correct)
   - Tenant code in header matches token claims
   - User has required permission for the endpoint
5. **Data Filtering** → All queries automatically scoped to user's tenant

### Permission Matrix

| Module | View | Create | Update | Delete | Custom |
|--------|------|--------|--------|--------|--------|
| **User** | `USER_VIEW` | `USER_CREATE` | `USER_UPDATE` | `USER_DELETE` | — |
| **Role** | `ROLE_VIEW` | `ROLE_CREATE` | `ROLE_UPDATE` | `ROLE_DELETE` | — |
| **Order** | `LOGIFLOW_ORDER_VIEW` | `LOGIFLOW_ORDER_CREATE` | `LOGIFLOW_ORDER_UPDATE` | — | Assign, Tracking |
| **Customer** | `LOGIFLOW_CUSTOMER_VIEW` | `LOGIFLOW_CUSTOMER_CREATE` | `LOGIFLOW_CUSTOMER_UPDATE` | `LOGIFLOW_CUSTOMER_DELETE` | — |
| **Driver** | `LOGIFLOW_DRIVER_VIEW` | `LOGIFLOW_DRIVER_CREATE` | `LOGIFLOW_DRIVER_UPDATE` | `LOGIFLOW_DRIVER_DELETE` | — |
| **Vehicle** | `LOGIFLOW_VEHICLE_VIEW` | `LOGIFLOW_VEHICLE_CREATE` | `LOGIFLOW_VEHICLE_UPDATE` | `LOGIFLOW_VEHICLE_DELETE` | — |
| **COD** | `LOGIFLOW_COD_VIEW` | — | `LOGIFLOW_COD_UPDATE` | — | — |
| **Reconciliation** | `LOGIFLOW_RECONCILIATION_VIEW` | `LOGIFLOW_RECONCILIATION_CREATE` | `LOGIFLOW_RECONCILIATION_UPDATE` | — | — |

---

## 🗄️ Database Schema Overview

### Core Service Database
```
tenants (tenant master)
├── users
│   ├── user_roles → roles
│   │   └── role_permissions → permissions
│   └── refresh_tokens (session mgmt)
├── audit_logs
├── files
└── notifications
```

### LogiFlow Service Database
```
logiflow_customers (customer master)

logiflow_drivers (driver master)
└── users (FK: user_id for driver authentication)

logiflow_vehicles (vehicle master)

logiflow_orders (core business)
├── logiflow_delivery_assignments (driver + vehicle binding)
├── logiflow_tracking_events (GPS + status history)
└── logiflow_cod_records (payment tracking)
    └── logiflow_reconciliations (settlement batches)
```

### Key Design Rules
- **Tenant Column**: All tables have `tenant_code` (indexed for fast filtering)
- **Soft Deletes**: `deleted_at` + `deleted_by` columns (no hard deletes)
- **Timestamps**: `created_at`, `updated_at` automatically managed
- **Migrations**: Schema changes via Flyway in `core-service` only
- **Driver Authentication**: Each driver has `user_id` FK to enable login via shared auth service (/api/auth/login)

---

## 📊 Key Business Workflows

### Workflow 1: Complete Order Delivery
```
1. Create Order (status: NEW, receiver address, COD amount if any)
   POST /api/logiflow/orders
   
2. Assign Driver & Vehicle (links to order)
   POST /api/logiflow/orders/{orderId}/assign
   
3. Track Delivery (driver updates location/events)
   POST /api/logiflow/orders/{orderId}/tracking
   
4. Update Order Status
   PATCH /api/logiflow/orders/{orderId}/status → IN_TRANSIT
   
5. Delivery Complete (update status to COMPLETED)
   PATCH /api/logiflow/orders/{orderId}/status → COMPLETED
   
6. If COD: Record collected cash
   POST /api/logiflow/orders/{orderId}/cod → {amount, status: COLLECTED}
```

### Workflow 2: COD Settlement (Reconciliation)
```
1. View Collected COD Records (ready for settlement)
   GET /api/logiflow/reconciliations/eligible-cod
   
2. Create Reconciliation Batch (select which CODs to settle)
   POST /api/logiflow/reconciliations
   body: {driverId?, codRecordIds: [...]}
   
3. View Reconciliation Details
   GET /api/logiflow/reconciliations/{reconciliationId}
   
4. Approve Settlement (update status to RECONCILED)
   PATCH /api/logiflow/reconciliations/{reconciliationId}/status
   body: {status: RECONCILED}
   
5. Check COD Summary (verify amounts reconciled)
   GET /api/logiflow/operations/cod/summary
```

### Workflow 3: Driver Authentication & Order Management (Driver Web App)
```
1. Driver Login (using shared auth service)
   POST /api/auth/login
   body: {username: "demo.driver", password: "...", tenantCode: "demo"}
   → Returns JWT token with MEMBER role + logiflow permissions

2. Get Driver's Assigned Orders
   GET /api/logiflow/orders?status=IN_TRANSIT
   (Filtered by current user's driver profile automatically)
   
3. Update Order Tracking (GPS/Location)
   POST /api/logiflow/orders/{orderId}/tracking
   body: {latitude, longitude, eventType: "IN_TRANSIT", notes}
   
4. Update Order Status (Delivery Progress)
   PATCH /api/logiflow/orders/{orderId}/status
   body: {status: "COMPLETED"}
   
5. Record COD Payment
   POST /api/logiflow/orders/{orderId}/cod
   body: {amount: 1000000, status: "COLLECTED"}
```

### Workflow 4: Driver & Vehicle Management
```
1. Register New Driver
   POST /api/logiflow/drivers
   (Automatically creates user account for login)
   
2. Register New Vehicle
   POST /api/logiflow/vehicles
   
3. Assign Vehicle to Driver
   PUT /api/logiflow/drivers/{driverId}
   body: {assignedVehicleId: ...}
   
4. View All Drivers
   GET /api/logiflow/drivers?status=ACTIVE&page=0&size=10
   
5. View All Vehicles
   GET /api/logiflow/vehicles?status=ACTIVE&page=0&size=10
```

---

## 🚀 Tech Stack

| Layer | Technology |
|-------|------------|
| **API Framework** | Spring Boot 3.x, Spring Cloud Gateway |
| **Security** | Spring Security, JWT (jsonwebtoken) |
| **Database** | PostgreSQL, JPA/Hibernate, Flyway |
| **API Docs** | Swagger/OpenAPI 3.0 |
| **Architecture** | Maven multi-module, microservices pattern |

---

## 📋 Deployment & Runtime

### Service Start Order
1. Start `core-service` on port `8081`
2. Start `logiflow-service` on port `8082`
3. Start `gateway-service` on port `8080` (routes to above)

### Configuration
- Database: PostgreSQL (Neon)
- Environment Variable: `NEON_DB_PASSWORD` (no hardcoded secrets)
- JWT Signing: Symmetric key (configured in environment)

### Demo Credentials
| User Type | Identifier (Username/Email/Phone) | Password | Tenant Code | Role | Permissions |
|-----------|----------------------------------|----------|-------------|------|-------------|
| Admin | `demo.owner` (username) | `password` | `demo` | OWNER | All permissions |
| Driver | `demo.driver` (username) or `0909123456` (phone) | `password` | `demo` | MEMBER | `LOGIFLOW_ORDER_*`, `LOGIFLOW_ORDER_TRACKING` |

### Login Examples
**By Username:**
```json
POST /api/auth/login
{
  "identifier": "demo.owner",
  "password": "password",
  "tenantCode": "demo"
}
```

**By Phone:**
```json
POST /api/auth/login
{
  "identifier": "0909123456",
  "password": "password",
  "tenantCode": "demo"
}
```

**By Email:**
```json
POST /api/auth/login
{
  "identifier": "demo.driver@tenantcore.local",
  "password": "password",
  "tenantCode": "demo"
}
```

### Testing
- Use Postman collection: `postman/tenantcore-gateway-mvp.postman_collection.json`
- Base URL: `http://localhost:8080`
- Auto-populated variables: `accessToken`, `refreshToken`, `orderId`, `customerId`, etc.
- E2E script: `tools/e2e_reconciliation_gateway.ps1`
- Driver Login: POST `/api/auth/login` with `demo.driver` credentials to test driver web app flow

---

## ✅ Current Status (Stage 8 Complete)

### Delivered
- ✅ Multi-tenant auth (login, refresh, logout, roles, permissions)
- ✅ Flexible login identifier (username/email/phone)
- ✅ Order management (CRUD + status flow + assignment + tracking)
- ✅ Driver/Vehicle/Customer management (CRUD)
- ✅ COD tracking and reconciliation workflow
- ✅ Operations reporting (COD summary, daily, by-driver)
- ✅ JWT security enforcement (token validation, tenant mismatch check)
- ✅ Permission matrix enforcement (endpoint authorization)
- ✅ Swagger/OpenAPI documentation
- ✅ E2E automation tests
- ✅ Database migrations (Flyway)
- ✅ Driver authentication (driver login + user account linking)
- ✅ Notification API (list, detail, unread count, mark read)

### Ready for Stage 8
- Performance optimization?
- Enhanced reporting/analytics?
- Frontend integration?
- Deployment/CI-CD pipelines?
- Additional business features?

---

## 📚 Detailed Documentation

For comprehensive platform documentation, refer to:

- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - System architecture, user types, entity relationships, authentication flow
- **[BUSINESS_FEATURES_OUTLINE.md](./BUSINESS_FEATURES_OUTLINE.md)** - API endpoints, features, workflows, tech stack
- **[tenantcore_logiflow_mvp_guide_compact.md](./tenantcore_logiflow_mvp_guide_compact.md)** - Implementation guide

### Base URL
`http://localhost:8080` (through gateway)

### Common Headers
```
Authorization: Bearer {accessToken}
X-Tenant-Code: demo
Content-Type: application/json
```

### Health Checks
```bash
GET http://localhost:8080/api/core/health
GET http://localhost:8080/api/logiflow/health
```

### Swagger UI
```
Core Service: http://localhost:8081/swagger-ui/index.html
LogiFlow Service: http://localhost:8082/swagger-ui/index.html
```

---

**For questions or updates, refer to:**
- `tenantcore_logiflow_mvp_guide_compact.md` - Implementation guide
- `docs/logiflow-permission-matrix.md` - Detailed permission matrix
- `postman/` - API testing collection
