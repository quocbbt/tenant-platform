# TenantCore + LogiFlow MVP - Compact Guide (Unified Flow)

## 1) Goal and Scope
- Keep current Maven multi-module architecture, do not split new services in MVP.
- Unified runtime flow:
  - `gateway-service (8080)` -> `core-service (8081)` / `logiflow-service (8082)` -> Neon PostgreSQL.
- Strict multi-tenant:
  - Business tables include `tenant_code`.
  - Business queries must always filter by `tenant_code`.

## 2) Modules
- `common-lib`: shared DTO, exception, context, security constants.
- `core-service`: IAM + Flyway migrations.
- `logiflow-service`: logistics business APIs (DB-backed), Flyway disabled.
- `gateway-service`: route and gateway security/filter.

## 3) Technical Baseline
- Java 21, Maven multi-module.
- Spring Boot 3.5.x, Spring Cloud 2025.0.x.
- PostgreSQL (Neon), Spring Data JPA, Flyway in `core-service` only.

## 4) Runtime Config Rule
- Production: do not hard-code secrets.
- DB password is loaded from environment variable `NEON_DB_PASSWORD` in:
  - `core-service/src/main/resources/application.yaml`
  - `logiflow-service/src/main/resources/application.yaml`

## 5) Current Delivery Status (as of 2026-05-13)
- [x] Stage 1-6 completed (module baseline, common-lib, health, gateway routes, Flyway, auth mock to real).
- [x] Real auth flow on `core-service`:
  - `POST /api/auth/setup-password`
  - `POST /api/auth/login`
  - `GET /api/auth/me`
  - `POST /api/auth/refresh`
  - `POST /api/auth/logout`
- [x] Order flow on `logiflow-service` (tenant-aware):
  - create/get/list/update-status/assign/tracking/cod.
- [x] JWT security enforcement:
  - no token -> 401
  - tenant mismatch (`X-Tenant-Code` vs token) -> 403
  - endpoint permission checks enabled.
- [x] Refactor architecture separation per module:
  - `api` + `web` + `application` + `service`.
- [x] Customer flow implemented on `logiflow-service` (tenant-aware):
  - `POST /api/logiflow/customers`
  - `GET /api/logiflow/customers/{id}`
  - `GET /api/logiflow/customers?page=&size=&status=&keyword=`
  - `PUT /api/logiflow/customers/{id}`
  - `DELETE /api/logiflow/customers/{id}` (soft delete).
- [x] Customer permission enforcement implemented:
  - `LOGIFLOW_CUSTOMER_VIEW`
  - `LOGIFLOW_CUSTOMER_CREATE`
  - `LOGIFLOW_CUSTOMER_UPDATE`
  - `LOGIFLOW_CUSTOMER_DELETE`
- [x] Driver flow implemented on `logiflow-service` (tenant-aware):
  - `POST /api/logiflow/drivers`
  - `GET /api/logiflow/drivers/{id}`
  - `GET /api/logiflow/drivers?page=&size=&status=&keyword=`
  - `PUT /api/logiflow/drivers/{id}`
  - `DELETE /api/logiflow/drivers/{id}` (soft delete).
- [x] Driver permission enforcement implemented:
  - `LOGIFLOW_DRIVER_VIEW`
  - `LOGIFLOW_DRIVER_CREATE`
  - `LOGIFLOW_DRIVER_UPDATE`
  - `LOGIFLOW_DRIVER_DELETE`
- [x] Vehicle flow implemented on `logiflow-service` (tenant-aware):
  - `POST /api/logiflow/vehicles`
  - `GET /api/logiflow/vehicles/{id}`
  - `GET /api/logiflow/vehicles?page=&size=&status=&keyword=`
  - `PUT /api/logiflow/vehicles/{id}`
  - `DELETE /api/logiflow/vehicles/{id}` (soft delete).
- [x] Vehicle permission enforcement implemented:
  - `LOGIFLOW_VEHICLE_VIEW`
  - `LOGIFLOW_VEHICLE_CREATE`
  - `LOGIFLOW_VEHICLE_UPDATE`
  - `LOGIFLOW_VEHICLE_DELETE`
- [x] Operations read model implemented (tenant-aware):
  - `GET /api/logiflow/operations/cod/summary`
  - response includes COD total/pending/collected/reconciled amounts.
- [x] Reconciliation read model implemented (tenant-aware):
  - `GET /api/logiflow/reconciliations?page=&size=&status=&keyword=`
  - `GET /api/logiflow/reconciliations/{id}`
  - protected by `LOGIFLOW_RECONCILIATION_VIEW`.
- [x] Reconciliation write flow implemented (tenant-aware):
  - `POST /api/logiflow/reconciliations` (`LOGIFLOW_RECONCILIATION_CREATE`)
  - `PATCH /api/logiflow/reconciliations/{id}/status` (`LOGIFLOW_RECONCILIATION_UPDATE`)
  - supports linking COD records with reconciliation.
- [x] Focused integration tests added for critical security path (`logiflow-service`):
  - no token access on protected order endpoint -> forbidden
  - token without required permission -> forbidden
  - tenant header mismatch vs token claim -> forbidden
  - valid token + permission + tenant -> success
  - file: `logiflow-service/src/test/java/com/tenantcore/logiflowservice/security/OrderSecurityIntegrationTest.java`
- [x] Flyway migration `V005` applied successfully in `core-service`:
  - added `reconciliation_id` on `logiflow_cod_records`
  - added FK to `logiflow_reconciliations`
  - added index `(tenant_code, reconciliation_id)`
- [x] Gateway E2E reconciliation write flow verified:
  - login -> create order -> update COD (`COLLECTED`) -> create reconciliation -> update status (`RECONCILED`)
  - reconciliation detail returns `RECONCILED`
  - COD summary now reflects reconciled amount correctly.
- [x] Eligible COD read endpoint added (tenant-aware):
  - `GET /api/logiflow/reconciliations/eligible-cod?page=&size=&keyword=`
  - returns COD records with status `COLLECTED` and not linked to reconciliation.
- [x] Reconciliation write flow tests added (`logiflow-service`):
  - file: `src/test/java/com/tenantcore/logiflowservice/reconciliation/ReconciliationServiceTest.java`
  - covers:
    - create reconciliation aggregates amount + links COD records
    - reject invalid COD status when creating reconciliation
    - update status `RECONCILED` syncs COD records
    - update status `CANCELLED` unlinks/rolls back COD records
- [x] Gateway E2E automation script added and verified:
  - script: `tools/e2e_reconciliation_gateway.ps1`
  - flow: auth -> create order -> update COD -> list eligible COD -> create reconciliation -> update status RECONCILED -> verify COD summary
  - latest run result: SUCCESS (2026-05-12)
- [x] Authorization matrix integration tests expanded:
  - file: `src/test/java/com/tenantcore/logiflowservice/security/AuthorizationMatrixIntegrationTest.java`
  - covered modules/endpoints:
    - customer list (`LOGIFLOW_CUSTOMER_VIEW`)
    - driver list (`LOGIFLOW_DRIVER_VIEW`)
    - vehicle list (`LOGIFLOW_VEHICLE_VIEW`)
    - reconciliation list (`LOGIFLOW_RECONCILIATION_VIEW`)
    - COD summary (`LOGIFLOW_COD_VIEW`)
  - includes tenant mismatch forbidden case.
- [x] Write-permission integration tests added:
  - file: `src/test/java/com/tenantcore/logiflowservice/security/AuthorizationWriteIntegrationTest.java`
  - covered permissions:
    - customer: `CREATE/UPDATE/DELETE`
    - driver: `CREATE/UPDATE/DELETE`
    - vehicle: `CREATE/UPDATE/DELETE`
    - reconciliation: `CREATE` + `UPDATE(status)`
  - verifies forbidden without permission and success with correct permission.
- [x] OpenAPI/Swagger enabled for `logiflow-service`:
  - OpenAPI JSON: `/v3/api-docs`
  - Swagger UI: `/swagger-ui/index.html`
  - security permits swagger endpoints as public.
- [x] Permission mapping table documented:
  - `docs/logiflow-permission-matrix.md`
- [x] Operations read-model expanded (date/driver analytics):
  - `GET /api/logiflow/operations/cod/daily?fromDate=&toDate=`
  - `GET /api/logiflow/operations/reconciliation/by-driver?fromDate=&toDate=`
  - default range: last 7 days, max 31 days.
- [x] OpenAPI descriptions/tags added for key operations/reconciliation endpoints.
- [x] Reconciliation create policy hardened:
  - optional `driverId` must belong to tenant, be `ACTIVE`, and match latest order assignment of every COD record in batch.
  - COD records included in reconciliation must satisfy configurable time-window policy (`app.reconciliation.policy.max-cod-age-hours`).
- [x] OpenAPI request/response examples extended for reconciliation write endpoints:
  - `POST /api/logiflow/reconciliations`
  - `PATCH /api/logiflow/reconciliations/{id}/status`
- [x] Stage 7 closed (2026-05-13):
  - all Stage 7 checklist items are finalized in implementation guide;
  - Stage 7 DoD is considered satisfied and guide status is synchronized.

## 6) Postman Through Gateway
- File: `postman/tenantcore-gateway-mvp.postman_collection.json`
- Base variables:
  - `baseUrl = http://localhost:8080`
  - `tenantCode = demo`
- Auto variables:
  - `accessToken`, `refreshToken`, `orderId`, `customerId`, `driverId`, `vehicleId`, `reconciliationId`
- Includes end-to-end requests for:
  - health, auth, order, customer, driver, vehicle, operations summary, reconciliation read/write flows via gateway.

## 7) Mandatory Local Run Order
1. Start `core-service` on `8081`.
2. Start `logiflow-service` on `8082`.
3. Start `gateway-service` on `8080`.
4. Verify through gateway first:
  - `/api/core/health`, `/api/logiflow/health`
  - auth flow -> order/customer business flow.

## 8) Migration Rule
- Migration files live in `core-service/src/main/resources/db/migration`.
- Never edit applied migration on live DB.
- Schema change must be a new migration version.

## 9) Next Priority Backlog (single stream)
1. [x] Stabilize reconciliation APIs:
   - stronger validation by driver/time-window policy has been implemented.
2. [x] Add request/response examples for create/update endpoints directly via OpenAPI annotations.

## 10) Definition of Done for Stage 7
- Full project build passes.
- Real auth works through gateway.
- Tenant filtering enforced on business queries.
- No secret leakage in committed runtime config.
- Guide is updated after every completed milestone.
