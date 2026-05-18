# Laundry Hub – Backend Service Specification

> **Phiên bản:** 1.0 · **Ngày:** 18/5/2026
> **Phạm vi:** Giai đoạn 1 – Single store, single operator, no IoT
> **Đọc bởi:** Backend Developer

---

## Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Infrastructure & Shared Components](#2-infrastructure--shared-components)
3. [Core Services](#3-core-services)
4. [Domain Services](#4-domain-services)
5. [Platform Services](#5-platform-services)
6. [Event Flow & Message Queue](#6-event-flow--message-queue)
7. [Background Jobs](#7-background-jobs)
8. [Error Handling & Edge Cases](#8-error-handling--edge-cases)
9. [Conventions & Standards](#9-conventions--standards)

---

## 1. Tổng quan kiến trúc

### 1.1 Service map (Giai đoạn 1)

```
                    ┌─────────────────────────────┐
                    │         API Gateway          │
                    │  JWT validate · Rate limit   │
                    └──────────────┬──────────────┘
                                   │
          ┌────────────────────────┼────────────────────────┐
          │                        │                        │
   ┌──────▼──────┐        ┌────────▼───────┐      ┌────────▼───────┐
   │ CORE (6)    │        │ DOMAIN (3)     │      │ PLATFORM (4)   │
   │             │        │                │      │                │
   │ order       │        │ staff          │      │ auth           │
   │ laundry-    │        │ kpi-payroll    │      │ payment        │
   │  process    │        │ vehicle-rental │      │ notification   │
   │ delivery    │        └────────────────┘      │ report-        │
   │ customer    │                                │  analytics     │
   │ label-print │        ┌────────────────┐      └────────────────┘
   │ inventory   │        │ INFRA          │
   └─────────────┘        │ PostgreSQL     │
                          │ Redis          │
                          │ Message Queue  │
                          │ Logging        │
                          └────────────────┘
```

> **Lưu ý giai đoạn 1:** `machine-service` và `review-service` được gộp vào `laundry-process-service` và `order-service` tương ứng. Tổng còn **13 service** thay vì 19.

### 1.2 Service list (giai đoạn 1)

| # | Service | Port | Gộp từ |
|---|---------|------|---------|
| 1 | `order-service` | 3001 | order + review |
| 2 | `laundry-process-service` | 3002 | laundry-process + machine + label-print |
| 3 | `delivery-service` | 3003 | — |
| 4 | `customer-service` | 3004 | — |
| 5 | `inventory-service` | 3005 | — |
| 6 | `shop-service` | 3006 | inventory + vehicle-rental gộp 1 service nhẹ |
| 7 | `staff-service` | 3007 | — |
| 8 | `kpi-payroll-service` | 3008 | — |
| 9 | `vehicle-rental-service` | 3009 | — |
| 10 | `auth-service` | 3010 | — |
| 11 | `payment-service` | 3011 | — |
| 12 | `notification-service` | 3012 | — |
| 13 | `report-analytics-service` | 3013 | — |

### 1.3 Tech stack

| Layer | Tech |
|-------|------|
| Runtime | Node.js 20 LTS |
| Framework | Fastify v4 |
| ORM | Prisma |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Queue | BullMQ (Redis-backed) |
| Auth | JWT (RS256) |
| Realtime | SSE (Server-Sent Events) |
| Validation | Zod |
| Testing | Vitest + Supertest |

---

## 2. Infrastructure & Shared Components

### 2.1 PostgreSQL – Database schema conventions

- Mọi table đều có: `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`, `created_at`, `updated_at`
- Soft delete: `deleted_at TIMESTAMPTZ NULL` (không xoá vật lý)
- Giai đoạn 1: **shared DB**, mỗi service có schema riêng trong cùng 1 Postgres instance
- `branch_id` chưa bắt buộc giai đoạn 1 nhưng **thêm vào ngay từ đầu** để dễ scale

```sql
-- Naming conventions
table_name: snake_case, số nhiều (orders, item_groups, staff_members)
foreign key: <table_singular>_id (order_id, staff_id)
index: idx_<table>_<column>
enum type: <domain>_<name>_enum (order_status_enum, contract_type_enum)
```

### 2.2 Redis – Key conventions

```
session:<user_id>              TTL: 24h   - JWT session
machine:status:<machine_id>    TTL: 5min  - Machine state cache
order:stream:<order_id>        TTL: 2h    - SSE subscriber count
lock:machine:<machine_id>      TTL: 30s   - Optimistic lock khi gán lô
ratelimit:<ip>:<endpoint>      TTL: 1min  - Rate limiting
```

### 2.3 Message Queue – Queue names

```
queue.order.created          → trigger: notification, kpi
queue.order.status_changed   → trigger: notification, kpi, report
queue.payment.completed      → trigger: order status update
queue.review.submitted       → trigger: kpi (rating score)
queue.attendance.checked_in  → trigger: kpi (punctuality)
queue.payroll.generate       → cron job trigger cuối tháng
queue.stock.low_alert        → trigger: notification admin
```

### 2.4 Shared middleware (API Gateway)

```typescript
// Áp dụng cho mọi request vào service
interface RequestContext {
  requestId: string;      // UUID per request, pass qua X-Request-ID header
  userId: string;
  role: 'admin' | 'manager' | 'supervisor' | 'staff' | 'customer';
  staffId?: string;
  branchId: string;       // Giai đoạn 1: luôn là 'branch_main'
  iat: number;
  exp: number;
}
```

---

## 3. Core Services

---

### 3.1 `order-service` · Port 3001

**Trách nhiệm:** Tạo đơn, quản lý vòng đời trạng thái, review, lịch sử đơn.

#### DB Schema

```sql
-- Đơn hàng
CREATE TABLE orders (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  branch_id       TEXT NOT NULL DEFAULT 'branch_main',
  customer_id     UUID NOT NULL REFERENCES customers(id),
  status          order_status_enum NOT NULL DEFAULT 'pending',
  delivery_type   delivery_type_enum NOT NULL,  -- pickup | delivery | cod | internal
  total_amount    INTEGER NOT NULL DEFAULT 0,   -- VND, tính sau khi cân thực tế
  total_kg        NUMERIC(6,2),
  note            TEXT,
  estimated_ready_at  TIMESTAMPTZ,
  delivered_at        TIMESTAMPTZ,
  cancelled_at        TIMESTAMPTZ,
  cancel_reason       TEXT,
  created_by      UUID,   -- staff_id nếu walk-in, null nếu customer tự đặt
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at      TIMESTAMPTZ
);

CREATE TYPE order_status_enum AS ENUM (
  'pending','confirmed','received','classified',
  'washing','drying','folding','packed',
  'dispatched','dispatched_cod','delivered','done','cancelled'
);

CREATE TYPE delivery_type_enum AS ENUM ('pickup','delivery','cod','internal');

-- Nhóm đồ trong đơn (1 đơn có nhiều nhóm)
CREATE TABLE order_item_groups (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id        UUID NOT NULL REFERENCES orders(id),
  category_id     UUID NOT NULL REFERENCES item_categories(id),
  service_type    service_type_enum NOT NULL,  -- wash_dry | wash_dry_iron | iron_only
  estimated_kg    NUMERIC(6,2),
  actual_kg       NUMERIC(6,2),               -- cập nhật khi nhân viên cân thực tế
  ironing_fee     INTEGER NOT NULL DEFAULT 0,
  subtotal        INTEGER NOT NULL DEFAULT 0,
  notes           TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE service_type_enum AS ENUM ('wash_dry','wash_dry_iron','iron_only');

-- Add-on của từng nhóm đồ
CREATE TABLE order_item_group_addons (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  item_group_id   UUID NOT NULL REFERENCES order_item_groups(id),
  addon_id        UUID NOT NULL REFERENCES addon_catalog(id),
  price_snapshot  INTEGER NOT NULL  -- giá tại thời điểm tạo đơn
);

-- Lịch sử thay đổi trạng thái
CREATE TABLE order_status_logs (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id    UUID NOT NULL REFERENCES orders(id),
  from_status order_status_enum,
  to_status   order_status_enum NOT NULL,
  staff_id    UUID REFERENCES staff_members(id),
  note        TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Review đơn hàng
CREATE TABLE order_reviews (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id    UUID NOT NULL UNIQUE REFERENCES orders(id),
  customer_id UUID NOT NULL REFERENCES customers(id),
  staff_id    UUID REFERENCES staff_members(id),  -- nhân viên chính xử lý đơn
  rating      SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment     TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Danh mục loại đồ
CREATE TABLE item_categories (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name            TEXT NOT NULL,
  price_per_kg    INTEGER NOT NULL,
  wash_program    TEXT,
  is_active       BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order      SMALLINT NOT NULL DEFAULT 0,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Danh mục add-on
CREATE TABLE addon_catalog (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name        TEXT NOT NULL,
  type        addon_type_enum NOT NULL,   -- detergent | softener | other
  price       INTEGER NOT NULL DEFAULT 0,
  price_type  TEXT NOT NULL DEFAULT 'flat',  -- flat | per_kg
  is_default  BOOLEAN NOT NULL DEFAULT FALSE,
  is_active   BOOLEAN NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE addon_type_enum AS ENUM ('detergent','softener','other');

-- Indexes
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_order_item_groups_order_id ON order_item_groups(order_id);
CREATE INDEX idx_order_reviews_staff_id ON order_reviews(staff_id);
```

#### API Endpoints

```
POST   /orders                        Tạo đơn mới
GET    /orders                        Danh sách đơn (filter: status, date, customer_id)
GET    /orders/:id                    Chi tiết đơn + item_groups + status_logs
PATCH  /orders/:id/status             Chuyển trạng thái
POST   /orders/:id/cancel             Huỷ đơn
GET    /orders/:id/stream             SSE – realtime tracking
POST   /orders/:id/review             Khách gửi đánh giá
GET    /admin/orders                  Danh sách có filter nâng cao (Admin/Manager)
PATCH  /admin/orders/:id              Admin can thiệp: chỉnh sửa, đổi nhân viên
GET    /item-categories               Danh sách loại đồ active
GET    /admin/item-categories         CRUD (Admin)
POST   /admin/item-categories
PATCH  /admin/item-categories/:id
GET    /addons                        Danh sách add-on active
GET    /admin/addons                  CRUD add-on (Admin)
POST   /admin/addons
PATCH  /admin/addons/:id
```

#### Request / Response contracts

```typescript
// POST /orders
interface CreateOrderBody {
  customer_id?: string;          // null nếu walk-in chưa có tài khoản
  customer_name?: string;        // walk-in
  customer_phone?: string;       // walk-in
  delivery_type: 'pickup' | 'delivery' | 'cod' | 'internal';
  estimated_ready_at?: string;   // ISO8601
  note?: string;
  item_groups: Array<{
    category_id: string;
    service_type: 'wash_dry' | 'wash_dry_iron' | 'iron_only';
    estimated_kg: number;
    addon_ids: string[];
    notes?: string;
  }>;
}

// PATCH /orders/:id/status
interface UpdateStatusBody {
  status: OrderStatus;
  staff_id: string;
  actual_kg?: number;            // bắt buộc khi → received
  note?: string;
}

// POST /orders/:id/review
interface SubmitReviewBody {
  rating: 1 | 2 | 3 | 4 | 5;
  comment?: string;
}
```

#### State machine – Allowed transitions

```typescript
const ALLOWED_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  pending:        ['confirmed', 'cancelled'],
  confirmed:      ['received', 'cancelled'],
  received:       ['classified'],
  classified:     ['washing'],
  washing:        ['drying'],
  drying:         ['folding'],
  folding:        ['packed'],
  packed:         ['dispatched', 'dispatched_cod', 'delivered'],
  dispatched:     ['delivered'],
  dispatched_cod: ['delivered'],
  delivered:      ['done'],
  done:           [],
  cancelled:      [],
};

// Validate trước khi update
function canTransition(from: OrderStatus, to: OrderStatus): boolean {
  return ALLOWED_TRANSITIONS[from]?.includes(to) ?? false;
}
```

#### Events published

```typescript
// Sau mỗi status change
queue.order.status_changed → {
  orderId, fromStatus, toStatus,
  staffId, customerId, timestamp
}

// Sau khi review được submit
queue.review.submitted → {
  orderId, staffId, rating, timestamp
}
```

---

### 3.2 `laundry-process-service` · Port 3002

**Trách nhiệm:** Nhận đồ → phân loại → gán máy → quản lý lô → in nhãn QR. Bao gồm machine management.

#### DB Schema

```sql
-- Máy giặt / sấy
CREATE TABLE machines (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  branch_id   TEXT NOT NULL DEFAULT 'branch_main',
  name        TEXT NOT NULL,               -- "Máy giặt 1", "Máy sấy 1"
  type        machine_type_enum NOT NULL,  -- washer | dryer
  capacity_kg NUMERIC(5,1) NOT NULL,
  status      machine_status_enum NOT NULL DEFAULT 'idle',
  version     INTEGER NOT NULL DEFAULT 0,  -- optimistic lock
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE machine_type_enum AS ENUM ('washer','dryer');
CREATE TYPE machine_status_enum AS ENUM ('idle','running','maintenance','error');

-- Lô giặt
CREATE TABLE laundry_batches (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  batch_code      TEXT NOT NULL UNIQUE,    -- "BATCH-2405-001"
  machine_id      UUID NOT NULL REFERENCES machines(id),
  status          batch_status_enum NOT NULL DEFAULT 'running',
  started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  finished_at     TIMESTAMPTZ,
  staff_id        UUID NOT NULL REFERENCES staff_members(id),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE batch_status_enum AS ENUM ('running','done','error');

-- Nhóm đồ trong lô (M2M: order_item_group ↔ batch)
CREATE TABLE batch_item_groups (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  batch_id        UUID NOT NULL REFERENCES laundry_batches(id),
  item_group_id   UUID NOT NULL REFERENCES order_item_groups(id),
  UNIQUE(batch_id, item_group_id)
);

-- Nhãn QR
CREATE TABLE labels (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  type        label_type_enum NOT NULL,   -- order | batch | package
  ref_id      UUID NOT NULL,              -- order_id / batch_id / item_group_id
  qr_payload  TEXT NOT NULL,             -- JSON string được encode
  printed_at  TIMESTAMPTZ,
  printed_by  UUID REFERENCES staff_members(id),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE label_type_enum AS ENUM ('order','batch','package');

-- Indexes
CREATE INDEX idx_machines_status ON machines(status);
CREATE INDEX idx_batches_machine_id ON laundry_batches(machine_id);
CREATE INDEX idx_labels_ref_id ON labels(ref_id);
```

#### API Endpoints

```
GET    /machines                      Danh sách máy + status realtime
GET    /machines/:id                  Chi tiết máy + lô hiện tại
POST   /batches                       Tạo lô giặt mới + gán máy
PATCH  /batches/:id/finish            Kết thúc lô, giải phóng máy
GET    /batches/:id                   Chi tiết lô + danh sách đơn

POST   /labels/order/:orderId         In nhãn đơn
POST   /labels/batch/:batchId         In nhãn lô
POST   /labels/package/:itemGroupId   In nhãn gói
GET    /labels/scan/:qrPayload        Lookup từ QR scan → trả về entity

POST   /process/receive/:orderId      Nhận đồ: cân + xác nhận actual_kg
POST   /process/classify/:orderId     Phân loại đơn
POST   /process/assign-batch          Gán item_group vào lô + chọn máy
```

#### Optimistic lock khi gán máy

```typescript
async function assignMachine(machineId: string, batchId: string) {
  // 1. Lock Redis trước
  const lockKey = `lock:machine:${machineId}`;
  const locked = await redis.set(lockKey, batchId, 'NX', 'EX', 30);
  if (!locked) throw new ConflictError('Machine is being assigned by another request');

  try {
    // 2. Lấy version hiện tại
    const machine = await db.machine.findUnique({ where: { id: machineId } });
    if (machine.status !== 'idle') throw new ConflictError('Machine not available');

    // 3. Update với version check
    const updated = await db.machine.updateMany({
      where: { id: machineId, version: machine.version },
      data: { status: 'running', version: { increment: 1 } }
    });

    if (updated.count === 0) throw new ConflictError('Machine state changed, retry');

    // 4. Gắn batch vào machine
    await db.laundryBatch.update({ where: { id: batchId }, data: { machineId } });
  } finally {
    await redis.del(lockKey);
  }
}
```

#### QR Payload format

```typescript
interface QRPayload {
  type: 'order' | 'batch' | 'package';
  id: string;
  ref: string;    // order code "ORD-2405-001" để hiển thị
  ts: number;     // unix timestamp tạo label
}
// Encode: base64url(JSON.stringify(payload))
```

---

### 3.3 `delivery-service` · Port 3003

**Trách nhiệm:** Phân loại và quản lý luồng giao trả, COD tracking.

#### DB Schema

```sql
CREATE TABLE dispatches (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id        UUID NOT NULL UNIQUE REFERENCES orders(id),
  type            delivery_type_enum NOT NULL,
  partner         TEXT,              -- "Grab" | "Gojek" | null
  tracking_code   TEXT,              -- mã vận đơn đối tác
  cod_fee         INTEGER,           -- VND
  cod_partner     TEXT,
  status          dispatch_status_enum NOT NULL DEFAULT 'pending',
  dispatched_at   TIMESTAMPTZ,
  delivered_at    TIMESTAMPTZ,
  staff_id        UUID NOT NULL REFERENCES staff_members(id),
  note            TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE dispatch_status_enum AS ENUM ('pending','dispatched','delivered','failed');

-- Indexes
CREATE INDEX idx_dispatches_order_id ON dispatches(order_id);
CREATE INDEX idx_dispatches_status ON dispatches(status);
```

#### API Endpoints

```
POST   /orders/:id/dispatch           Tạo dispatch (chọn loại giao)
PATCH  /orders/:id/dispatch/:dispId   Cập nhật tracking code
PATCH  /orders/:id/complete           Xác nhận khách đã nhận → order = delivered
GET    /orders/:id/dispatch           Xem dispatch hiện tại
GET    /admin/dispatches              Danh sách giao hàng (filter: date, type, status)
```

#### Request contracts

```typescript
// POST /orders/:id/dispatch
interface CreateDispatchBody {
  type: 'pickup' | 'delivery' | 'cod' | 'internal';
  partner?: string;         // bắt buộc nếu type = 'cod'
  cod_fee?: number;         // bắt buộc nếu type = 'cod'
  note?: string;
}

// Validation rules
if (body.type === 'cod') {
  assert(body.partner, 'partner is required for COD');
  assert(body.cod_fee > 0, 'cod_fee must be positive');
}
```

#### Events published

```typescript
queue.order.status_changed → { orderId, toStatus: 'dispatched_cod' | 'delivered', ... }
```

---

### 3.4 `customer-service` · Port 3004

**Trách nhiệm:** Hồ sơ khách hàng, ghi nợ, loyalty points, sổ địa chỉ.

#### DB Schema

```sql
CREATE TABLE customers (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  phone           TEXT NOT NULL UNIQUE,
  name            TEXT NOT NULL,
  email           TEXT,
  avatar_url      TEXT,
  type            customer_type_enum NOT NULL DEFAULT 'regular',
  loyalty_points  INTEGER NOT NULL DEFAULT 0,
  debt_amount     INTEGER NOT NULL DEFAULT 0,     -- VND, số dương = đang nợ
  debt_limit      INTEGER NOT NULL DEFAULT 500000, -- hạn mức nợ tối đa
  is_blocked      BOOLEAN NOT NULL DEFAULT FALSE,
  note            TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE customer_type_enum AS ENUM ('tourist','regular','vip');

CREATE TABLE customer_addresses (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id UUID NOT NULL REFERENCES customers(id),
  label       TEXT NOT NULL,   -- "Nhà", "Khách sạn"
  address     TEXT NOT NULL,
  is_default  BOOLEAN NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE debt_transactions (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id UUID NOT NULL REFERENCES customers(id),
  order_id    UUID REFERENCES orders(id),
  amount      INTEGER NOT NULL,  -- dương = phát sinh nợ, âm = trả nợ
  note        TEXT,
  created_by  UUID,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE UNIQUE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_debt_transactions_customer_id ON debt_transactions(customer_id);
```

#### API Endpoints

```
GET    /customers                     Tìm kiếm khách (query: phone, name)
GET    /customers/:id                 Hồ sơ + lịch sử đơn + nợ
POST   /customers                     Tạo khách mới (walk-in)
PATCH  /customers/:id                 Cập nhật thông tin
POST   /customers/:id/debt            Ghi nợ / thanh toán nợ
GET    /customers/:id/debt            Lịch sử giao dịch nợ
GET    /customers/me                  Khách tự xem hồ sơ (Customer App)
PATCH  /customers/me                  Khách cập nhật thông tin
POST   /customers/me/addresses        Thêm địa chỉ
DELETE /customers/me/addresses/:id    Xoá địa chỉ
```

#### Debt guard middleware

```typescript
// Chạy trước POST /orders khi delivery_type = 'cod' hoặc có ghi nợ
async function checkDebtLimit(customerId: string, newDebtAmount: number) {
  const customer = await customerService.findById(customerId);
  if (customer.is_blocked) throw new ForbiddenError('Customer is blocked');
  if (customer.debt_amount + newDebtAmount > customer.debt_limit) {
    throw new PaymentRequiredError(`Debt limit exceeded: ${customer.debt_limit} VND`);
  }
}
```

---

### 3.5 `inventory-service` · Port 3005

**Trách nhiệm:** Sản phẩm bán lẻ tại quầy, tồn kho, upsell.

#### DB Schema

```sql
CREATE TABLE products (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  branch_id           TEXT NOT NULL DEFAULT 'branch_main',
  name                TEXT NOT NULL,
  description         TEXT,
  price               INTEGER NOT NULL,
  stock               INTEGER NOT NULL DEFAULT 0,
  low_stock_threshold INTEGER NOT NULL DEFAULT 5,
  unit                TEXT NOT NULL DEFAULT 'cái',
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE stock_transactions (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id  UUID NOT NULL REFERENCES products(id),
  type        stock_tx_type_enum NOT NULL,  -- sale | restock | adjustment
  quantity    INTEGER NOT NULL,             -- âm = xuất, dương = nhập
  order_id    UUID REFERENCES orders(id),  -- null nếu restock
  note        TEXT,
  staff_id    UUID REFERENCES staff_members(id),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE stock_tx_type_enum AS ENUM ('sale','restock','adjustment');

-- Indexes
CREATE INDEX idx_products_is_active ON products(is_active);
CREATE INDEX idx_stock_tx_product_id ON stock_transactions(product_id);
```

#### API Endpoints

```
GET    /products                      Danh sách sản phẩm active
GET    /products/:id                  Chi tiết + lịch sử tồn kho
POST   /products/sell                 Bán sản phẩm (gắn order_id nếu kèm đơn)
GET    /admin/products                CRUD (Admin/Manager)
POST   /admin/products
PATCH  /admin/products/:id
POST   /admin/products/:id/restock    Nhập hàng
GET    /admin/stock-transactions      Lịch sử xuất nhập kho
```

#### Low stock event

```typescript
// Sau mỗi lần bán, kiểm tra ngưỡng tồn kho
async function afterSale(productId: string) {
  const product = await db.product.findUnique({ where: { id: productId } });
  if (product.stock <= product.low_stock_threshold) {
    await queue.publish('queue.stock.low_alert', {
      productId, productName: product.name,
      currentStock: product.stock, threshold: product.low_stock_threshold
    });
  }
}
```

---

## 4. Domain Services

---

### 4.1 `staff-service` · Port 3007

**Trách nhiệm:** Hồ sơ nhân viên, ca làm, chấm công, OT.

#### DB Schema

```sql
CREATE TABLE staff_members (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  branch_id       TEXT NOT NULL DEFAULT 'branch_main',
  user_id         UUID UNIQUE REFERENCES users(id),  -- link đến auth-service
  name            TEXT NOT NULL,
  phone           TEXT NOT NULL UNIQUE,
  pin_hash        TEXT NOT NULL,               -- bcrypt hash của PIN 6 số
  role            staff_role_enum NOT NULL DEFAULT 'staff',
  contract_type   contract_type_enum NOT NULL DEFAULT 'hourly',
  hourly_rate     INTEGER,                     -- VND/giờ (nếu hourly)
  base_salary     INTEGER,                     -- VND/tháng (nếu fixed)
  is_active       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE staff_role_enum AS ENUM ('admin','manager','supervisor','staff');
CREATE TYPE contract_type_enum AS ENUM ('hourly','fixed');

CREATE TABLE shifts (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  staff_id    UUID NOT NULL REFERENCES staff_members(id),
  date        DATE NOT NULL,
  start_time  TIME NOT NULL,
  end_time    TIME NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE attendance_logs (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  staff_id    UUID NOT NULL REFERENCES staff_members(id),
  shift_id    UUID REFERENCES shifts(id),
  checked_in_at   TIMESTAMPTZ,
  checked_out_at  TIMESTAMPTZ,
  total_minutes   INTEGER,        -- tính sau khi checkout
  is_late         BOOLEAN NOT NULL DEFAULT FALSE,
  late_minutes    INTEGER NOT NULL DEFAULT 0,
  note            TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_attendance_staff_date ON attendance_logs(staff_id, checked_in_at);
CREATE INDEX idx_shifts_staff_date ON shifts(staff_id, date);
```

#### API Endpoints

```
GET    /staff                         Danh sách nhân viên (Admin/Manager)
GET    /staff/:id                     Hồ sơ + ca làm + chấm công
POST   /staff                         Tạo nhân viên mới (Admin)
PATCH  /staff/:id                     Cập nhật hồ sơ
DELETE /staff/:id                     Vô hiệu hoá tài khoản

POST   /attendance/checkin            Body: { staff_id }
POST   /attendance/checkout           Body: { staff_id }
GET    /attendance                    Danh sách chấm công (filter: staff_id, date)
PATCH  /admin/attendance/:id          Admin chỉnh sửa chấm công

GET    /shifts                        Lịch ca (filter: staff_id, date_range)
POST   /admin/shifts                  Tạo ca làm
PATCH  /admin/shifts/:id              Sửa ca làm
```

#### Events published

```typescript
// Khi checkin
queue.attendance.checked_in → {
  staffId, shiftId, checkedInAt,
  isLate: boolean, lateMinutes: number
}
```

---

### 4.2 `kpi-payroll-service` · Port 3008

**Trách nhiệm:** Lắng nghe events, tổng hợp KPI real-time, tính lương, chu kỳ lương.

#### DB Schema

```sql
CREATE TABLE kpi_events (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  staff_id    UUID NOT NULL REFERENCES staff_members(id),
  event_type  kpi_event_type_enum NOT NULL,
  ref_id      UUID,       -- order_id / review_id / attendance_id
  value       NUMERIC(10,2) NOT NULL,  -- số đơn +1, rating 4.5, sales 120000
  period      TEXT NOT NULL,           -- "2026-05" (tháng)
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE kpi_event_type_enum AS ENUM (
  'order_completed','review_rating','sales_amount','on_time_delivery','attendance'
);

CREATE TABLE kpi_summaries (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  staff_id            UUID NOT NULL REFERENCES staff_members(id),
  period              TEXT NOT NULL,              -- "2026-05"
  orders_completed    INTEGER NOT NULL DEFAULT 0,
  avg_rating          NUMERIC(3,2) NOT NULL DEFAULT 0,
  sales_amount        INTEGER NOT NULL DEFAULT 0,
  on_time_rate        NUMERIC(5,4) NOT NULL DEFAULT 0,
  attendance_score    NUMERIC(5,4) NOT NULL DEFAULT 0,
  kpi_score           NUMERIC(5,2) NOT NULL DEFAULT 0,  -- 0-100
  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(staff_id, period)
);

CREATE TABLE payrolls (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  staff_id        UUID NOT NULL REFERENCES staff_members(id),
  period          TEXT NOT NULL,               -- "2026-05"
  contract_type   contract_type_enum NOT NULL,
  total_hours     NUMERIC(6,2),               -- null nếu fixed
  hourly_rate     INTEGER,
  base_salary     INTEGER,
  kpi_score       NUMERIC(5,2) NOT NULL,
  kpi_bonus       INTEGER NOT NULL DEFAULT 0,
  total_amount    INTEGER NOT NULL,
  status          payroll_status_enum NOT NULL DEFAULT 'draft',
  approved_by     UUID REFERENCES staff_members(id),
  approved_at     TIMESTAMPTZ,
  paid_at         TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(staff_id, period)
);

CREATE TYPE payroll_status_enum AS ENUM ('draft','approved','paid');

-- Indexes
CREATE INDEX idx_kpi_events_staff_period ON kpi_events(staff_id, period);
CREATE INDEX idx_kpi_summaries_period ON kpi_summaries(period);
CREATE INDEX idx_payrolls_period_status ON payrolls(period, status);
```

#### KPI calculation engine

```typescript
// KPI weights (có thể cấu hình qua Admin Dashboard)
interface KPIWeights {
  orders_completed: number;   // default 0.30
  avg_rating: number;         // default 0.30
  sales_amount: number;       // default 0.20
  on_time_rate: number;       // default 0.15
  attendance: number;         // default 0.05
}

function calculateKPIScore(summary: KPISummary, weights: KPIWeights): number {
  // Normalize từng chỉ tiêu về thang 0-100 trước khi nhân trọng số
  const ordersScore = Math.min(summary.orders_completed / 200 * 100, 100);
  const ratingScore = ((summary.avg_rating - 1) / 4) * 100;
  const salesScore = Math.min(summary.sales_amount / 5000000 * 100, 100);
  const onTimeScore = summary.on_time_rate * 100;
  const attendanceScore = summary.attendance_score * 100;

  return (
    ordersScore   * weights.orders_completed +
    ratingScore   * weights.avg_rating +
    salesScore    * weights.sales_amount +
    onTimeScore   * weights.on_time_rate +
    attendanceScore * weights.attendance
  );
}

// Tính lương hourly
function calculateHourlyPayroll(totalHours: number, hourlyRate: number, kpiScore: number): number {
  const base = totalHours * hourlyRate;
  const bonusRate = kpiScore >= 80 ? 0.15 : kpiScore >= 60 ? 0.08 : 0;
  return Math.round(base * (1 + bonusRate));
}
```

#### Event consumers

```typescript
// Lắng nghe từ queue
consumers = {
  'queue.order.status_changed': async (event) => {
    if (event.toStatus === 'done') {
      await kpiService.addEvent(event.staffId, 'order_completed', event.orderId, 1);
    }
    if (['delivered', 'done'].includes(event.toStatus)) {
      const isOnTime = await checkOnTime(event.orderId);
      await kpiService.addEvent(event.staffId, 'on_time_delivery', event.orderId, isOnTime ? 1 : 0);
    }
  },
  'queue.review.submitted': async (event) => {
    await kpiService.addEvent(event.staffId, 'review_rating', event.orderId, event.rating);
  },
  'queue.attendance.checked_in': async (event) => {
    const score = event.isLate ? Math.max(0, 1 - event.lateMinutes / 60) : 1;
    await kpiService.addEvent(event.staffId, 'attendance', event.shiftId, score);
  },
};
```

#### API Endpoints

```
GET    /kpi/:staffId                  KPI summary (query: period=2026-05)
GET    /kpi/:staffId/events           Event log chi tiết
GET    /admin/kpi                     KPI tất cả nhân viên trong kỳ
GET    /admin/payroll                 Danh sách bảng lương (query: period, status)
GET    /admin/payroll/:staffId        Bảng lương 1 nhân viên
PATCH  /admin/payroll/:id/approve     Phê duyệt (Admin only)
PATCH  /admin/payroll/:id/paid        Đánh dấu đã trả
GET    /admin/config/kpi-weights      Lấy cấu hình trọng số
PUT    /admin/config/kpi-weights      Cập nhật trọng số (Admin only)
```

---

### 4.3 `vehicle-rental-service` · Port 3009

**Trách nhiệm:** Quản lý xe, đặt/trả xe, hợp đồng, phí phát sinh.

#### DB Schema

```sql
CREATE TABLE vehicles (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  branch_id       TEXT NOT NULL DEFAULT 'branch_main',
  name            TEXT NOT NULL,           -- "Honda Vision"
  type            vehicle_type_enum NOT NULL,
  plate           TEXT NOT NULL UNIQUE,
  daily_rate      INTEGER NOT NULL,        -- VND/ngày
  deposit_amount  INTEGER NOT NULL,
  status          vehicle_status_enum NOT NULL DEFAULT 'available',
  notes           TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE vehicle_type_enum AS ENUM ('motorbike','electric','bicycle');
CREATE TYPE vehicle_status_enum AS ENUM ('available','rented','maintenance');

CREATE TABLE rental_contracts (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  vehicle_id      UUID NOT NULL REFERENCES vehicles(id),
  customer_id     UUID NOT NULL REFERENCES customers(id),
  staff_id        UUID NOT NULL REFERENCES staff_members(id),
  start_date      DATE NOT NULL,
  end_date        DATE NOT NULL,           -- ngày dự kiến trả
  actual_end_date DATE,                   -- ngày trả thực tế
  daily_rate      INTEGER NOT NULL,        -- snapshot tại thời điểm ký
  deposit_amount  INTEGER NOT NULL,
  total_amount    INTEGER,                 -- tính sau khi trả xe
  extra_fees      INTEGER NOT NULL DEFAULT 0,
  extra_fee_note  TEXT,
  deposit_refunded INTEGER,               -- số tiền hoàn cọc thực tế
  status          rental_status_enum NOT NULL DEFAULT 'active',
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE rental_status_enum AS ENUM ('active','returned','overdue','cancelled');

-- Indexes
CREATE INDEX idx_vehicles_status ON vehicles(status);
CREATE INDEX idx_rentals_vehicle_id ON rental_contracts(vehicle_id);
CREATE INDEX idx_rentals_customer_id ON rental_contracts(customer_id);
CREATE INDEX idx_rentals_status ON rental_contracts(status);
```

#### API Endpoints

```
GET    /vehicles                      Danh sách xe (query: status=available)
GET    /vehicles/:id                  Chi tiết xe + lịch sử thuê
POST   /rentals                       Tạo hợp đồng thuê
GET    /rentals/:id                   Chi tiết hợp đồng
PATCH  /rentals/:id/return            Trả xe (ghi actual_end_date + extra_fees)
GET    /admin/vehicles                CRUD xe (Admin/Manager)
POST   /admin/vehicles
PATCH  /admin/vehicles/:id
GET    /admin/rentals                 Danh sách hợp đồng (filter: status, date)
```

#### Late return auto-calculation

```typescript
// Cron job chạy mỗi ngày 00:01 - kiểm tra xe trễ hạn
async function checkOverdueRentals() {
  const today = new Date();
  const overdueRentals = await db.rentalContract.findMany({
    where: { status: 'active', end_date: { lt: today } }
  });

  for (const rental of overdueRentals) {
    const overdueDays = differenceInDays(today, rental.end_date);
    const extraFee = overdueDays * rental.daily_rate * 1.5; // phí trễ = 150% ngày thuê

    await db.rentalContract.update({
      where: { id: rental.id },
      data: { status: 'overdue', extra_fees: extraFee,
              extra_fee_note: `Trễ ${overdueDays} ngày` }
    });
    await queue.publish('queue.rental.overdue', { rentalId: rental.id, customerId: rental.customer_id, overdueDays });
  }
}
```

---

## 5. Platform Services

---

### 5.1 `auth-service` · Port 3010

**Trách nhiệm:** Đăng ký/đăng nhập, JWT, phân quyền, OTP.

#### DB Schema

```sql
CREATE TABLE users (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  phone           TEXT UNIQUE,
  email           TEXT UNIQUE,
  role            TEXT NOT NULL DEFAULT 'customer',  -- customer | staff | admin | manager | supervisor
  is_active       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE otp_requests (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  phone       TEXT NOT NULL,
  otp_hash    TEXT NOT NULL,       -- bcrypt hash
  expires_at  TIMESTAMPTZ NOT NULL,
  used        BOOLEAN NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

#### JWT structure

```typescript
interface JWTPayload {
  sub: string;          // user_id
  role: string;         // 'customer' | 'staff' | 'admin' | 'manager' | 'supervisor'
  staffId?: string;     // nếu role = staff/*
  branchId: string;     // 'branch_main' (giai đoạn 1)
  iat: number;
  exp: number;          // 24h với customer, 12h với staff
}
```

#### API Endpoints

```
POST   /auth/otp/send              Gửi OTP qua SMS
POST   /auth/otp/verify            Xác thực OTP → trả token
POST   /auth/staff/login           Staff login bằng ID + PIN
POST   /auth/refresh               Refresh token
POST   /auth/logout                Revoke token (xoá Redis session)
GET    /auth/me                    Thông tin user hiện tại
```

#### OTP flow

```typescript
// Rate limit: max 3 OTP request/phone/5 phút
const OTP_TTL = 5 * 60; // 300 giây
const OTP_LENGTH = 6;

async function sendOTP(phone: string): Promise<void> {
  const otp = generateNumericOTP(OTP_LENGTH);
  const hash = await bcrypt.hash(otp, 10);
  const expiresAt = addSeconds(new Date(), OTP_TTL);

  await db.otpRequest.create({ data: { phone, otp_hash: hash, expires_at: expiresAt } });
  await smsProvider.send(phone, `Mã OTP Laundry Hub: ${otp}. Hiệu lực 5 phút.`);
}
```

---

### 5.2 `payment-service` · Port 3011

**Trách nhiệm:** Xử lý thanh toán, QR payment, ghi nợ, webhook.

#### DB Schema

```sql
CREATE TABLE payments (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id        UUID REFERENCES orders(id),
  rental_id       UUID REFERENCES rental_contracts(id),
  method          payment_method_enum NOT NULL,
  amount          INTEGER NOT NULL,
  status          payment_status_enum NOT NULL DEFAULT 'pending',
  provider_ref    TEXT,            -- mã giao dịch từ Momo/ZaloPay
  qr_code_url     TEXT,
  webhook_data    JSONB,
  paid_at         TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE payment_method_enum AS ENUM ('cash','qr_momo','qr_zalopay','qr_vietqr','debt','cod');
CREATE TYPE payment_status_enum AS ENUM ('pending','completed','failed','refunded');

-- Indexes
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
```

#### API Endpoints

```
POST   /payments/qr                   Tạo QR payment (Momo/ZaloPay/VietQR)
POST   /payments/cash                 Ghi nhận tiền mặt
POST   /payments/debt                 Ghi nợ cho khách
GET    /payments/:id                  Trạng thái thanh toán
POST   /webhooks/momo                 Webhook từ Momo
POST   /webhooks/zalopay              Webhook từ ZaloPay
```

#### Events published

```typescript
// Sau khi payment completed
queue.payment.completed → {
  paymentId, orderId, method, amount, paidAt
}
```

#### Retry logic cho QR payment

```typescript
// Job kiểm tra QR payment pending > 15 phút → expire
async function expireStalePendingPayments() {
  const stalePayments = await db.payment.findMany({
    where: {
      status: 'pending',
      method: { in: ['qr_momo', 'qr_zalopay', 'qr_vietqr'] },
      created_at: { lt: subMinutes(new Date(), 15) }
    }
  });
  for (const p of stalePayments) {
    await db.payment.update({ where: { id: p.id }, data: { status: 'failed' } });
  }
}
```

---

### 5.3 `notification-service` · Port 3012

**Trách nhiệm:** Push notification, Zalo OA, SMS, thông báo nội bộ.

#### DB Schema

```sql
CREATE TABLE notifications (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  recipient   TEXT NOT NULL,         -- user_id hoặc phone
  channel     notification_channel_enum NOT NULL,
  type        TEXT NOT NULL,         -- 'order_status', 'review_reminder', 'overdue', etc.
  title       TEXT NOT NULL,
  body        TEXT NOT NULL,
  data        JSONB,                 -- extra payload
  status      TEXT NOT NULL DEFAULT 'pending',  -- pending | sent | failed
  sent_at     TIMESTAMPTZ,
  error       TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE notification_channel_enum AS ENUM ('push','zalo','sms','internal');
```

#### Event consumers → notification triggers

```typescript
const NOTIFICATION_TRIGGERS = {
  'queue.order.status_changed': async (event) => {
    // Push tới customer khi đơn thay đổi trạng thái
    await notify(event.customerId, 'push', 'order_status', {
      title: `Đơn ${event.orderId}`,
      body: STATUS_MESSAGES[event.toStatus]
    });

    // Internal alert nếu đơn trễ
    if (event.isOverdue) {
      await notify('admin', 'internal', 'overdue_order', {
        title: 'Đơn trễ hạn',
        body: `Đơn ${event.orderId} đã trễ ${event.overdueMins} phút`
      });
    }
  },
  'queue.stock.low_alert': async (event) => {
    await notify('admin', 'internal', 'low_stock', {
      title: 'Tồn kho thấp',
      body: `${event.productName}: còn ${event.currentStock} ${event.unit}`
    });
  }
};

// Review reminder – cron job sau 1h kể từ delivered
async function sendReviewReminders() {
  const deliveredOrders = await db.order.findMany({
    where: {
      status: 'done',
      delivered_at: { lt: subHours(new Date(), 1) },
      review: null
    }
  });
  for (const order of deliveredOrders) {
    await notify(order.customer_id, 'push', 'review_reminder', {
      title: 'Đánh giá dịch vụ',
      body: 'Hãy cho chúng tôi biết trải nghiệm của bạn!'
    });
  }
}
```

---

### 5.4 `report-analytics-service` · Port 3013

**Trách nhiệm:** Tổng hợp báo cáo doanh thu, vận hành, KPI, xuất Excel/PDF.

#### API Endpoints

```
GET    /admin/reports/revenue         Doanh thu (query: period=7d|30d|month|custom)
GET    /admin/reports/operations      Vận hành: số đơn, kg xử lý, tốc độ
GET    /admin/reports/customers       LTV, retention, top khách
GET    /admin/reports/staff-kpi       Xếp hạng KPI nhân viên theo kỳ
GET    /admin/reports/cod             Tổng phí COD theo đối tác
GET    /admin/dashboard               Live metrics tổng quan
GET    /admin/reports/export          Xuất Excel (query: type, period)
```

#### Revenue response contract

```typescript
interface RevenueReport {
  period: string;
  total: number;
  breakdown: {
    laundry: number;
    vehicle_rental: number;
    products: number;
    cod_fees_deducted: number;   // trừ ra
    net_revenue: number;
  };
  daily: Array<{
    date: string;
    laundry: number;
    rental: number;
    products: number;
  }>;
  comparison: {
    previous_period: number;
    growth_rate: number;         // % tăng/giảm
  };
}
```

---

## 6. Event Flow & Message Queue

### 6.1 Full event diagram

```
Customer đặt đơn
      │
      ▼
[order-service] ──publish──► queue.order.created
      │                              │
      │                    ┌─────────┴──────────┐
      │              [notification]        [report]
      │
      ▼
Staff nhận đồ → PATCH /orders/:id/status (received)
      │
      ▼
[order-service] ──publish──► queue.order.status_changed { toStatus: 'received' }
      │                              │
      │              ┌───────────────┼──────────────┐
      │         [notification]  [kpi-payroll]   [report]
      │               (push)    (+order event)
      ▼
... (các bước giặt, sấy, gấp, đóng gói)
      │
      ▼
Staff giao → PATCH /orders/:id/status (delivered)
      │
      ▼
[order-service] ──publish──► queue.order.status_changed { toStatus: 'delivered' }
      │
      ├──► [kpi-payroll] → +delivery_count, check on_time
      ├──► [notification] → push "Đơn đã giao"
      │
      ▼ (sau 1h)
[notification CRON] ──► queue → push review reminder
      │
      ▼
Customer submit review
      │
      ▼
[order-service] ──publish──► queue.review.submitted
      │
      └──► [kpi-payroll] → +rating score

Payment completed
      │
      ▼
[payment-service] ──publish──► queue.payment.completed
      │
      └──► [order-service] → update order payment_status
```

### 6.2 Queue configuration (BullMQ)

```typescript
const QUEUE_CONFIG = {
  'queue.order.status_changed': {
    attempts: 3,
    backoff: { type: 'exponential', delay: 1000 },
    removeOnComplete: { age: 86400 },   // giữ 24h
    removeOnFail: { age: 604800 }       // giữ 7 ngày nếu fail
  },
  'queue.payment.completed': {
    attempts: 5,
    backoff: { type: 'exponential', delay: 2000 },
    priority: 1   // cao nhất
  },
  'queue.notification.*': {
    attempts: 3,
    backoff: { type: 'fixed', delay: 5000 }
  }
};
```

---

## 7. Background Jobs

| Job | Schedule | Service | Mô tả |
|-----|----------|---------|-------|
| `expireQRPayments` | Mỗi 5 phút | payment | Expire QR payment pending > 15 phút |
| `sendReviewReminders` | Mỗi 30 phút | notification | Push review sau 1h giao hàng |
| `checkOverdueOrders` | Mỗi 15 phút | order | Alert đơn quá estimated_ready_at |
| `checkOverdueRentals` | Hàng ngày 00:01 | vehicle-rental | Tính phí xe trễ hạn |
| `generateMonthlyPayroll` | Ngày cuối tháng 23:00 | kpi-payroll | Tạo draft bảng lương |
| `recalcKPISummary` | Mỗi 1 giờ | kpi-payroll | Tổng hợp lại KPI summary từ events |
| `sendShiftReminders` | Hàng ngày 07:00 | notification | Nhắc ca làm hôm nay cho nhân viên |

---

## 8. Error Handling & Edge Cases

### 8.1 HTTP Error codes chuẩn

```typescript
// Sử dụng nhất quán toàn hệ thống
400 Bad Request       // Validation error – trả về chi tiết field lỗi
401 Unauthorized      // JWT invalid hoặc expired
403 Forbidden         // Không có quyền (role không đủ)
404 Not Found         // Resource không tồn tại
409 Conflict          // Race condition, duplicate, state không hợp lệ
422 Unprocessable     // Business logic error (vd: huỷ đơn đang giặt)
429 Too Many Requests // Rate limit
500 Internal Error    // Unexpected – log đầy đủ, không leak stack trace

// Error response format
{
  "error": {
    "code": "ORDER_INVALID_STATUS_TRANSITION",
    "message": "Cannot cancel order in 'washing' status",
    "field": null,           // null hoặc tên field nếu validation error
    "requestId": "uuid"
  }
}
```

### 8.2 Edge cases quan trọng

#### Huỷ đơn sau khi đã nhận đồ (Q1)
```typescript
// Chỉ huỷ được khi status < 'washing'
const CANCELLABLE_STATUSES = ['pending', 'confirmed', 'received', 'classified'];

async function cancelOrder(orderId: string, staffId: string) {
  const order = await db.order.findUnique({ where: { id: orderId } });
  if (!CANCELLABLE_STATUSES.includes(order.status)) {
    throw new UnprocessableError('ORDER_INVALID_STATUS_TRANSITION',
      `Order cannot be cancelled in '${order.status}' status`);
  }
  // Tính tiền hoàn: 100% nếu chưa received, 50% nếu received/classified
  const refundRate = order.status === 'pending' || order.status === 'confirmed' ? 1.0 : 0.5;
  // ...
}
```

#### Thanh toán QR thất bại (R1)
```typescript
// Sau khi QR expire hoặc fail, order giữ nguyên status
// FE hiển thị nút "Thử lại" → tạo QR mới hoặc chuyển tiền mặt
// Không tự động cancel order khi payment fail
```

#### Khách vượt hạn mức nợ (R2)
```typescript
// Kiểm tra trước khi xác nhận đơn có delivery_type = 'cod' hoặc payment = 'debt'
// Nếu vượt: trả 422 với code DEBT_LIMIT_EXCEEDED
// FE: hiển thị warning dialog, yêu cầu thanh toán trước
```

#### Race condition gán máy
```typescript
// Xử lý bằng Redis lock + optimistic lock (đã mô tả ở laundry-process-service)
// Nếu lock thất bại: trả 409 Conflict → FE retry sau 1-2 giây
```

#### Giao nhầm đơn (Q3)
```typescript
// Nếu phát hiện sau khi delivered:
// 1. Admin tạo "swap request" thủ công
// 2. Order A và B đều set status = 'issue_reported'
// 3. Nhân viên liên hệ khách, xử lý ngoài hệ thống
// 4. Admin update lại status = 'done' sau khi giải quyết
// Không có auto-rollback vì đồ đã ra khỏi cửa hàng
```

---

## 9. Conventions & Standards

### 9.1 API conventions

```
- Base path: /api/v1/
- Pagination: ?page=1&limit=20 → { data, meta: { total, page, limit, totalPages } }
- Date filter: ?date_from=2026-05-01&date_to=2026-05-31 (ISO8601 date)
- Sort: ?sort_by=created_at&sort_order=desc
- Search: ?q=<keyword> (tìm kiếm text)
- Tất cả timestamp trả về: UTC ISO8601 (FE tự convert sang local)
- Tiền tệ: Integer VND (không dùng float, không dùng decimal)
```

### 9.2 Logging format

```typescript
// Mỗi request log đủ 4 trường
{
  requestId: string,
  level: 'info' | 'warn' | 'error',
  service: 'order-service',
  message: string,
  duration_ms: number,
  userId?: string,
  statusCode: number
}
```

### 9.3 Không implement giai đoạn 1

```
❌ IoT / kết nối máy giặt vật lý
❌ branch_id filtering (thêm field nhưng không filter)
❌ Multi-tenant routing tại API Gateway
❌ Super-admin role
❌ Webhook tự động từ Grab/Gojek API
❌ Schema-per-branch database isolation
❌ Branch-level pricing override
```

### 9.4 Chuẩn bị sẵn cho scale-up (thêm ngay từ đầu)

```
✅ branch_id trên mọi entity chính
✅ Optimistic locking (version column) trên Machine
✅ row-level filtering theo branch_id trong mọi query
✅ requestId tracking toàn hệ thống
✅ Event-driven architecture (dễ thêm consumer mới)
✅ Soft delete toàn bộ (không mất data lịch sử)
```
