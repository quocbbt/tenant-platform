# TenantCore + LogiFlow - Master Guide (Unified Stages)

**Last Updated:** 2026-05-15  
**Current Program Status:** ✅ Stage 1 -> Stage 8 completed

---

## 1) Mục tiêu tài liệu

Tài liệu này là **guide tổng hợp duy nhất** để theo dõi toàn bộ hành trình triển khai TenantCore + LogiFlow theo stage, từ nền tảng kỹ thuật đến vận hành Docker, đồng thời chuẩn hóa trạng thái hoàn thành để tránh lệch thông tin giữa nhiều file.

---

## 2) Phạm vi hệ thống MVP

- Kiến trúc Maven multi-module:
  - `common-lib`
  - `core-service` (IAM + Flyway)
  - `logiflow-service` (nghiệp vụ logistics)
  - `gateway-service` (routing + security filter)
- Runtime chính:
  - `gateway-service (8080)` -> `core-service (8081)` / `logiflow-service (8082)` -> PostgreSQL
- Nguyên tắc multi-tenant nghiêm ngặt:
  - Bảng nghiệp vụ có `tenant_code`
  - Truy vấn nghiệp vụ luôn lọc theo `tenant_code`

---

## 3) Trạng thái stage tổng hợp

| Stage | Trạng thái | Nội dung chính | Kết quả |
|---|---|---|---|
| 1 | ✅ Complete | Chuẩn hóa Maven multi-module | Build cấu trúc module ổn định |
| 2 | ✅ Complete | Hoàn thiện `common-lib` (DTO/exception/context/security constants) | Dùng chung chuẩn response + error handling |
| 3 | ✅ Complete | Service bootstrapping + health API + gateway route | 3 service chạy đúng cổng, route qua gateway |
| 4 | ✅ Complete | Kết nối PostgreSQL (Neon) + cấu hình env | Không hard-code secret, dùng env var |
| 5 | ✅ Complete | Flyway migration nền tảng | Schema core + logiflow được migrate |
| 6 | ✅ Complete | API mock ban đầu (auth/order) | Có luồng test sớm qua gateway |
| 7 | ✅ Complete (2026-05-13) | API thật + JWT + RBAC + tenant enforcement + flows nghiệp vụ | Auth thật, order/customer/driver/vehicle/COD/reconciliation hoạt động |
| 8 | ✅ Complete (2026-05-14) | Docker deployment infrastructure | Dockerfiles, compose stack, helper scripts, docs vận hành đầy đủ |

---

## 4) Chi tiết Stage 7 (Business + Security)

### 4.1 Auth (core-service)
- `POST /api/auth/setup-password`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

### 4.2 LogiFlow nghiệp vụ (tenant-aware)
- Orders: create/get/list/update-status/assign/tracking/cod
- Customers: create/get/list/update/delete (soft delete)
- Drivers: create/get/list/update/delete (soft delete)
- Vehicles: create/get/list/update/delete (soft delete)
- Reconciliation:
  - list/get/create/update-status
  - eligible COD endpoint
- Operations read-model:
  - COD summary
  - COD daily
  - reconciliation by driver

### 4.3 Security enforcement
- JWT bắt buộc cho protected endpoints
- Header `X-Tenant-Code` phải khớp tenant trong token
- Kiểm tra permission theo endpoint
- Có integration tests cho authz matrix + write permissions + tenant mismatch

---

## 5) Chi tiết Stage 8 (Docker Deployment)

### 5.1 Deliverables chính
- Dockerfiles multi-stage cho:
  - `core-service`
  - `logiflow-service`
  - `gateway-service`
- `docker-compose-new.yml` (PostgreSQL + 3 services + health checks)
- `.env.example`, `.dockerignore`
- Helper scripts:
  - `docker-helper.bat` (Windows)
  - `docker-helper.sh` (Linux/Mac)
- Bộ tài liệu Docker (quick start + deployment guide + completion summary)

### 5.2 Kết quả vận hành
- Khởi động stack đồng bộ bằng Docker Compose
- Quản lý phụ thuộc startup theo health checks
- Dùng env var cho secrets/config
- Sẵn sàng cho local deployment/testing và tiền đề production hardening

---

## 6) Quy tắc kỹ thuật bắt buộc (duy trì xuyên stage)

- Không hard-code password/secret trong source
- Chỉ `core-service` chạy Flyway migration
- Không sửa migration đã apply trên môi trường thật; mọi thay đổi schema là migration mới
- Tuân thủ tenant isolation ở mọi query nghiệp vụ
- Thực thi permission kiểm soát endpoint theo RBAC

---

## 7) Definition of Done đã đạt

### Stage 7 DoD
- Build toàn dự án thành công
- Auth thực chạy qua gateway
- Tenant filtering áp dụng cho truy vấn nghiệp vụ
- Không lộ secret trong runtime config đã commit

### Stage 8 DoD
- Dockerfiles + compose stack đầy đủ
- Health checks và service dependencies hoạt động
- Migrations tự chạy khi stack khởi tạo
- Gateway route đúng tới core/logiflow
- Dữ liệu DB persist qua volume

---

## 8) Backlog đề xuất Stage 9+

1. CI/CD pipeline (build-test-scan-deploy tự động)
2. Kubernetes manifests/Helm
3. Monitoring + alerting (Prometheus/Grafana)
4. Centralized logging (ELK/OpenSearch)
5. Load testing + performance tuning
6. Security hardening nâng cao (mTLS, network policy, secrets management)
7. Database backup & disaster recovery drill
8. API gateway enhancements (rate limiting, caching)

---

## 9) Tài liệu nguồn tham chiếu

- `docs/BUSINESS_FEATURES_OUTLINE.md`
- `docs/DOCKER_DEPLOYMENT_GUIDE.md`
- `docs/FE_API_INTEGRATION_GUIDE.md`
- `docs/logiflow-permission-matrix.md`
