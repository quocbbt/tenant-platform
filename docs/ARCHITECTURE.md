# TenantCore Platform - Architecture & Design

## 🏗️ System Overview

TenantCore is a **multi-tenant logistics management platform** with three microservices:

1. **Gateway Service** (port 8080) - API routing + authentication
2. **Core Service** (port 8081) - User management, auth, permissions, notifications
3. **LogiFlow Service** (port 8082) - Logistics operations (orders, drivers, vehicles, customers)

---

## 👥 User Types & Architecture

The platform distinguishes between **3 types of users** with different purposes, data models, and capabilities:

### 1️⃣ **User (Internal System User)**

**Definition:** Internal platform users who manage the system (admins, staff, super-admins).

**Database:** `core-service.users`

**Schema:**
```sql
users
├── id (UUID, PK)
├── tenant_code (VARCHAR, indexed)
├── username (VARCHAR, unique per tenant)
├── email (VARCHAR, unique per tenant)
├── phone (VARCHAR, optional)
├── password_hash (VARCHAR, bcrypt)
├── full_name (VARCHAR)
├── status (ACTIVE/INACTIVE/LOCKED)
├── created_at, updated_at
└── deleted_at (soft delete)
```

**Authentication:**
- ✅ Can login via `/api/auth/login`
- ✅ Identifier: username OR email OR phone (case-insensitive username/email)
- ✅ Password-based authentication (bcrypt)
- ✅ Returns JWT token with roles + permissions

**Authorization:**
- ✅ RBAC roles: OWNER, ADMIN, MEMBER
- ✅ Permission-based access (e.g., `LOGIFLOW_ORDER_VIEW`, `USER_CREATE`)
- ✅ All users subject to tenant isolation

**Use Cases:**
- Admin/Owner: Full system access
- Staff/Member: Limited access to specific operations
- Super-Admin: Multi-tenant management

**Example:**
```
Username: demo.owner
Email: demo.owner@tenantcore.local
Phone: (optional)
Role: OWNER
Permissions: ALL
```

---

### 2️⃣ **Driver (Logistics Operator)**

**Definition:** Platform operators who deliver orders and track logistics.

**Database:** `logiflow-service.logiflow_drivers`

**Schema:**
```sql
logiflow_drivers
├── id (UUID, PK)
├── tenant_code (VARCHAR, indexed)
├── driver_code (VARCHAR, unique per tenant)
├── full_name (VARCHAR)
├── phone (VARCHAR)
├── email (VARCHAR)
├── license_number (VARCHAR)
├── status (ACTIVE/INACTIVE)
├── user_id (UUID, FK → users.id) ✅ KEY LINK
├── created_at, updated_at
└── deleted_at (soft delete)
```

**Authentication:**
- ✅ Can login via `/api/auth/login` (through `user_id` FK)
- ✅ Identifier: username OR email OR phone (from linked user account)
- ✅ Password stored in `users` table (via FK)
- ✅ Returns JWT token with role MEMBER + LOGIFLOW_* permissions

**Authorization:**
- ✅ Role: MEMBER (implied by driver profile)
- ✅ Permissions: `LOGIFLOW_ORDER_VIEW`, `LOGIFLOW_ORDER_UPDATE`, `LOGIFLOW_ORDER_TRACKING`, `LOGIFLOW_COD_UPDATE`
- ✅ Tenant isolation: scoped by `logiflow_drivers.tenant_code`

**Relationship:**
- **1 driver = 1 user account** (exclusive)
- Driver records linked to users table via `user_id` FK
- When creating driver, must create corresponding user account
- Driver login uses the linked user's credentials

**Use Cases:**
- Deliver packages
- Update order status + tracking
- Record GPS/location events
- Collect COD payment

**Example:**
```
Driver Code: DRV-001
Full Name: Demo Driver
Phone: 0909123456
Email: demo.driver@tenantcore.local
User ID: 22222222-2222-2222-2222-222222222222 (FK to users)
Status: ACTIVE

Login: identifier = "demo.driver" OR "0909123456"
       password = (from linked user account)
```

---

### 3️⃣ **Customer (External Order Sender)**

**Definition:** External customers who send/create orders but do NOT operate the system.

**Database:** `logiflow-service.logiflow_customers`

**Schema:**
```sql
logiflow_customers
├── id (UUID, PK)
├── tenant_code (VARCHAR, indexed)
├── customer_code (VARCHAR, unique per tenant)
├── customer_name (VARCHAR)
├── phone (VARCHAR)
├── email (VARCHAR)
├── address (TEXT)
├── type (NORMAL/VIP/BUSINESS)
├── status (ACTIVE/INACTIVE)
├── created_at, updated_at
└── deleted_at (soft delete)
```

**Authentication:**
- ❌ **Cannot login** (currently)
- ❌ No user account
- ❌ No password stored
- ❌ No JWT token

**Authorization:**
- ❌ No roles/permissions
- ❌ No access to system APIs

**Use Cases (Current - No Login):**
- Used as metadata when creating orders (sender info)
- Tracked for reporting/analytics
- Contacted for delivery confirmation

**Future Enhancement (Not Implemented):**
- Public tracking link: `/tracking/{orderId}?token=xxx` (no login needed)
- Customer portal: Create user account → login → view own orders
- Notification subscriptions: Email/SMS updates on order status

**Example:**
```
Customer Code: CUST-001
Name: Nguyen Van A (Shop Owner)
Phone: 0909999888
Email: shop@example.com
Address: 123 Nguyen Hue, District 1, HCMC
Type: BUSINESS
Status: ACTIVE
```

---

## 📊 Comparison Table

| Aspect | User | Driver | Customer |
|--------|------|--------|----------|
| **Service** | Core Service | LogiFlow Service | LogiFlow Service |
| **Table** | `users` | `logiflow_drivers` | `logiflow_customers` |
| **Has Password?** | ✅ Yes | ✅ Yes (via FK user) | ❌ No |
| **Has User Account?** | ✅ Yes | ✅ Yes (FK: user_id) | ❌ No |
| **Can Login?** | ✅ Yes | ✅ Yes | ❌ No (currently) |
| **Has Roles?** | ✅ Yes (OWNER, ADMIN, MEMBER) | ✅ Yes (MEMBER) | ❌ No |
| **Has Permissions?** | ✅ Yes | ✅ Yes (LOGIFLOW_*) | ❌ No |
| **Login Identifier** | username, email, phone | username, email, phone | N/A |
| **Tenant Scope** | `users.tenant_code` | `logiflow_drivers.tenant_code` | `logiflow_customers.tenant_code` |
| **Tenant Isolation** | ✅ Automatic | ✅ Automatic | ✅ Automatic |
| **Primary Purpose** | System administration | Order delivery | Order creation |

---

## 🔄 Entity Relationships

```
Core Service:
├─ tenants
│  └─ users
│     ├─ user_roles → roles → role_permissions → permissions
│     ├─ refresh_tokens (session management)
│     └─ user_tenants (reserved for multi-tenant future)
└─ notifications
└─ audit_logs

LogiFlow Service:
├─ logiflow_customers (order senders, no auth)
│
├─ logiflow_drivers (delivery operators, auth via FK)
│  └─ user_id FK → core-service.users
│
├─ logiflow_vehicles (delivery vehicles)
│
└─ logiflow_orders (core business)
   ├─ logiflow_delivery_assignments (driver + vehicle binding)
   ├─ logiflow_tracking_events (GPS + status history)
   └─ logiflow_cod_records (payment tracking)
      └─ logiflow_reconciliations (settlement batches)
```

---

## 🔐 Authentication Flow

### **User / Driver Login Flow**
```
1. POST /api/auth/login
   body: {
     "identifier": "username/email/phone",
     "password": "...",
     "tenantCode": "demo"
   }

2. Core Service validates:
   - Query: findByTenantCodeAndIdentifierAndStatusAndDeletedAtIsNull()
   - Check: username OR email OR phone (case-insensitive for username/email)
   - Verify: password matches bcrypt hash
   - Resolve: roles + permissions from user_roles → roles → permissions

3. Return JWT Token:
   {
     "accessToken": "...",
     "refreshToken": "...",
     "expiresIn": 3600,
     "user": {
       "id": "...",
       "tenantCode": "demo",
       "username": "...",
       "fullName": "...",
       "roles": ["MEMBER"],
       "permissions": ["LOGIFLOW_ORDER_VIEW", ...]
     }
   }

4. Client stores JWT + uses in Authorization header
   Authorization: Bearer {accessToken}
```

### **Customer Portal (Future)**
```
Option 1 - Public Tracking (No Login):
POST /api/logiflow/orders/{orderId}/tracking/public?token=xxx
(No JWT required, token validates order ownership)

Option 2 - Customer Account Login:
1. Create customer user account (different role: CUSTOMER)
2. POST /api/auth/login with customer credentials
3. Grant read-only permission: LOGIFLOW_ORDER_VIEW
4. Customer can GET /api/logiflow/orders (filtered by customer_id)
```

---

## 🏗️ Tenant Isolation Design

**Rule:** All data is strictly tenant-isolated. No cross-tenant leakage.

**Implementation:**
1. Every table has `tenant_code` column (indexed)
2. Every query includes tenant filter: `WHERE tenant_code = :tenantCode`
3. JWT carries tenant claim: resolved from `users.tenant_code`
4. All APIs auto-scope: UserContext extracts tenantCode from JWT

**Validation:**
- Request `X-Tenant-Code` header MUST match JWT tenant claim
- If mismatch → reject with `TENANT_FORBIDDEN`
- If missing → use JWT tenant claim as default

---

## 🎯 Login Examples

### **User (Admin/Staff) Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Code: demo" \
  -d '{
    "identifier": "demo.owner",
    "password": "password"
  }'
```

### **Driver Login (by username):**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Code: demo" \
  -d '{
    "identifier": "demo.driver",
    "password": "password"
  }'
```

### **Driver Login (by phone):**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Code: demo" \
  -d '{
    "identifier": "0909123456",
    "password": "password"
  }'
```

### **Driver Login (by email):**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Code: demo" \
  -d '{
    "identifier": "demo.driver@tenantcore.local",
    "password": "password"
  }'
```

---

## 📝 Demo Data Summary

### Tenant: `demo`

**User Accounts (core-service.users):**
| Username | Email | Phone | Password | Role | Created |
|----------|-------|-------|----------|------|---------|
| `demo.owner` | demo.owner@tenantcore.local | (optional) | `password` | OWNER | V004 migration |
| `demo.driver` | demo.driver@tenantcore.local | 0909123456 | `password` | MEMBER | V007 migration |

**Driver Profile (logiflow-service.logiflow_drivers):**
| Driver Code | Full Name | Phone | Email | License | User ID | Status |
|-------------|-----------|-------|-------|---------|---------|--------|
| DRV-001 | Demo Driver | 0909123456 | demo.driver@tenantcore.local | DL-2024-001 | 22222222-... | ACTIVE |

---

## 🚀 Next Steps

1. **Customer Portal** (Future):
   - Decide: Public tracking link vs. Customer login
   - If login: Create CUSTOMER role + permissions
   - If public: Implement token-based tracking endpoint

2. **Mobile App**:
   - Driver mobile app: Use same `/api/auth/login` flow
   - Customer app: Use public tracking or customer portal

3. **Multi-Tenant User** (Future):
   - Activate `user_tenants` table
   - Support user in multiple tenants
   - Implement tenant switching in login

---

## 📚 Related Documentation

- **BUSINESS_FEATURES_OUTLINE.md** - Feature list and endpoints
- **SECURITY_AND_ROLES.md** (to create) - Permission matrix details
- **.env.example** - Environment configuration
