# TenantCore Platform - Quick Reference Guide

## 📌 User Types Quick Summary

| Type | Who | Login | Use Case |
|------|-----|-------|----------|
| **User** | Admin/Staff | ✅ Yes (username/email/phone) | Manage system, orders, staff |
| **Driver** | Delivery Operator | ✅ Yes (via linked user account) | Deliver orders, track, update status |
| **Customer** | Order Sender | ❌ No (future: public link or portal) | Send orders, view status |

**Key:** Driver has `user_id` FK linking to user account for authentication.

---

## 🔑 Demo Login Credentials

**Tenant:** `demo`

```
Admin:
  Username: demo.owner
  Password: password
  Identifier: demo.owner (or email/phone if added)
  Role: OWNER

Driver:
  Username: demo.driver
  Password: password
  Phone: 0909123456
  Identifier: demo.driver OR 0909123456 (or email)
  Role: MEMBER
  Can do: View/update orders, track, record COD
```

---

## 📡 API Base URL

```
http://localhost:8080  (Gateway)
  ├── /api/auth/**      → Core Service
  ├── /api/core/**      → Core Service
  ├── /api/notifications/** → Core Service
  └── /api/logiflow/**  → LogiFlow Service
```

---

## 🚀 Quick Start

### 1. Login
```bash
POST /api/auth/login
{
  "identifier": "demo.driver",
  "password": "password",
  "tenantCode": "demo"
}
```

### 2. Get My Profile
```bash
GET /api/auth/me
Headers: Authorization: Bearer {accessToken}
```

### 3. List My Orders (Driver)
```bash
GET /api/logiflow/orders
Headers:
  Authorization: Bearer {accessToken}
  X-Tenant-Code: demo
Params: ?status=IN_TRANSIT&page=0&size=10
```

### 4. Update Order Tracking
```bash
POST /api/logiflow/orders/{orderId}/tracking
Headers:
  Authorization: Bearer {accessToken}
  X-Tenant-Code: demo
Body: {
  "latitude": 10.7769,
  "longitude": 106.6966,
  "eventType": "IN_TRANSIT",
  "notes": "Arrived at customer location"
}
```

### 5. Mark Notification as Read
```bash
PUT /api/notifications/{notificationId}/read
Headers:
  Authorization: Bearer {accessToken}
  X-Tenant-Code: demo
```

---

## 🔐 Authentication Details

**Login Identifier:** Can use any of:
- Username: `demo.owner`
- Email: `demo.owner@tenantcore.local`
- Phone: `0909123456`

**Token Expiry:**
- Access Token: 1 hour (3600 seconds)
- Refresh Token: 7 days (604800 seconds)

**Refresh Token:**
```bash
POST /api/auth/refresh
{
  "refreshToken": "{refreshToken}"
}
```

**Logout:**
```bash
POST /api/auth/logout
{
  "refreshToken": "{refreshToken}"
}
```

---

## 📊 Database Schema Quick Map

**Core Service:**
- `users` - User accounts (admin/staff/driver accounts)
- `tenants` - Tenant (company) master
- `roles` - RBAC roles (OWNER, ADMIN, MEMBER)
- `permissions` - Fine-grained permissions
- `user_roles` - User-Role mapping
- `role_permissions` - Role-Permission mapping
- `refresh_tokens` - Session tokens
- `notifications` - User notifications
- `audit_logs` - Action logs

**LogiFlow Service:**
- `logiflow_drivers` - Driver profiles (FK: user_id)
- `logiflow_customers` - Customer profiles (no auth)
- `logiflow_vehicles` - Vehicle profiles
- `logiflow_orders` - Orders
- `logiflow_delivery_assignments` - Driver-Order-Vehicle bindings
- `logiflow_tracking_events` - GPS tracking history
- `logiflow_cod_records` - COD payment records
- `logiflow_reconciliations` - Settlement batches

---

## 🎯 Common Scenarios

### **Admin Creates New Driver**
1. POST `/api/logiflow/drivers` - Create driver profile with username
2. Behind scenes: Automatically creates user account
3. Driver can login with username/email/phone

### **Driver Updates Order Status**
1. POST `/api/auth/login` with driver credentials
2. GET `/api/logiflow/orders` - List assigned orders
3. PATCH `/api/logiflow/orders/{id}/status` - Update status
4. POST `/api/logiflow/orders/{id}/tracking` - Record location

### **Driver Records COD Collection**
1. POST `/api/logiflow/orders/{id}/cod` 
```json
{
  "amount": 1000000,
  "status": "COLLECTED"
}
```

### **Admin Views Operations Report**
1. GET `/api/logiflow/operations/cod/summary` - COD stats
2. GET `/api/logiflow/operations/cod/daily` - Daily breakdown
3. GET `/api/logiflow/operations/reconciliations/driver-report` - By driver

---

## ⚙️ Configuration

**Environment Variables:**
```bash
NEON_DB_PASSWORD=your_neon_password
APP_JWT_SECRET=your_jwt_secret_key
```

**Service Ports:**
- Core Service: 8081
- LogiFlow Service: 8082
- Gateway: 8080

---

## 📖 Full Documentation

See **[ARCHITECTURE.md](./ARCHITECTURE.md)** for:
- Detailed user type definitions
- Entity relationships
- Authentication flows
- Future enhancements (customer portal, mobile app)

See **[BUSINESS_FEATURES_OUTLINE.md](./BUSINESS_FEATURES_OUTLINE.md)** for:
- Complete API endpoint list
- Permission matrix
- Business workflows
- Tech stack details

---

## 🆘 Troubleshooting

**Login fails:**
- Check: Is tenant code correct? (default: `demo`)
- Check: Is user account ACTIVE (not INACTIVE/LOCKED)?
- Check: Is password correct?

**Permission denied:**
- Check: User has the required permission?
- Check: Role assigned correctly?
- Check: Token still valid (< 1 hour)?

**Tenant mismatch error:**
- Check: `X-Tenant-Code` header matches JWT tenant
- Or omit header (will use JWT tenant)

**Order not visible:**
- Check: Only driver sees own assigned orders
- Admin sees all orders
