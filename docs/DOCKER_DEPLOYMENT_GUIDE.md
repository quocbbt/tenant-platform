# Docker Deployment Guide

## Overview

This guide explains how to deploy TenantCore + LogiFlow MVP using Docker and Docker Compose.

**Architecture:**
```
┌─────────────────────────────────────────────────┐
│         Gateway Service (8080)                  │
├──────────────┬──────────────────────────────────┤
│              │                                  │
│              ▼                                  ▼
│    Core Service (8081)                 LogiFlow Service (8082)
│    ├─ Auth API                         ├─ Order API
│    ├─ User Management                  ├─ Driver API
│    ├─ Roles & Permissions              ├─ Vehicle API
│    │                                   ├─ Customer API
│    │                                   ├─ Reconciliation API
│    │                                   └─ Operations API
│    └─ Flyway Migrations                └─ No Flyway
│              │                                  │
└──────────────┴──────────────────────────────────┘
              ▼
       PostgreSQL (5432)
       └─ Shared Database
```

## Prerequisites

- Docker (version 20.10+)
- Docker Compose (version 2.0+)
- At least 4GB available memory
- Ports available: 5432 (DB), 8080 (Gateway), 8081 (Core), 8082 (LogiFlow)

## Quick Start (Local Development)

### 1. Clone and Navigate
```bash
cd D:\jakie\tenant-platform
```

### 2. Build and Start Services
```bash
docker-compose -f docker-compose-new.yml up --build
```

**Output should show:**
```
✓ postgres service healthy
✓ core-service service started
✓ logiflow-service service started
✓ gateway-service service started
```

### 3. Verify Services
```bash
# Check all containers running
docker ps

# Test gateway health
curl http://localhost:8080/actuator/health

# Test core-service health
curl http://localhost:8081/actuator/health

# Test logiflow-service health
curl http://localhost:8082/actuator/health
```

### 4. Access APIs

**Through Gateway (Recommended):**
```
Base URL: http://localhost:8080
Auth Endpoints: POST /api/auth/login, POST /api/auth/logout, etc.
Order Endpoints: POST /api/logiflow/orders, GET /api/logiflow/orders, etc.
```

**Direct Service Access:**
```
Core Service: http://localhost:8081
LogiFlow Service: http://localhost:8082
```

### 5. Database Connection

```
Host: localhost
Port: 5432
Database: neondb
Username: neondb_owner
Password: postgres123 (default, customize via POSTGRES_PASSWORD env var)
```

## Configuration

### Environment Variables

Create a `.env` file in the project root:

```env
# Database
POSTGRES_PASSWORD=your-secure-password

# JWT Secret (IMPORTANT: Change in production)
APP_JWT_SECRET=your-super-secret-jwt-key-change-in-production

# Optional: Service URLs (for Docker-to-Docker communication)
CORE_SERVICE_URL=http://core-service:8081
LOGIFLOW_SERVICE_URL=http://logiflow-service:8082
```

**Apply custom configuration:**
```bash
docker-compose -f docker-compose-new.yml up --build
```

Docker Compose automatically loads variables from `.env` file.

### Service-Specific Configuration

**Core Service** (`core-service/src/main/resources/application.yaml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/neondb
    password: ${NEON_DB_PASSWORD}
  flyway:
    enabled: true  # Enables DB migrations on startup
```

**LogiFlow Service** (`logiflow-service/src/main/resources/application.yaml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/neondb
    password: ${NEON_DB_PASSWORD}
  flyway:
    enabled: false  # Migrations handled by core-service
```

**Gateway Service** (`gateway-service/src/main/resources/application.yaml`):
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: core-service
          uri: ${CORE_SERVICE_URL:http://localhost:8081}
        - id: logiflow-service
          uri: ${LOGIFLOW_SERVICE_URL:http://localhost:8082}
```

## Docker Image Details

### Build Process (Multi-Stage)

Each Dockerfile uses multi-stage builds for smaller final images:

**Stage 1 (Builder):**
- Base: `maven:3.9-eclipse-temurin-21`
- Compiles code with Maven
- Output: `.jar` file

**Stage 2 (Runtime):**
- Base: `eclipse-temurin:21-jre-alpine` (lightweight)
- Copies only the compiled JAR
- Smaller image size (~200MB per service)

### Health Checks

Each service includes health checks (via Spring Boot actuator):
```
Interval: 30 seconds
Timeout: 10 seconds
Retries: 3
Start period: 40-60 seconds
```

Health check endpoints:
- Core: `http://localhost:8081/actuator/health`
- LogiFlow: `http://localhost:8082/actuator/health`
- Gateway: `http://localhost:8080/actuator/health`

## Common Commands

### Start Services
```bash
# Start in foreground (see logs)
docker-compose -f docker-compose-new.yml up

# Start in background
docker-compose -f docker-compose-new.yml up -d

# Force rebuild images
docker-compose -f docker-compose-new.yml up --build --force-recreate
```

### Stop Services
```bash
# Graceful stop
docker-compose -f docker-compose-new.yml stop

# Stop and remove containers
docker-compose -f docker-compose-new.yml down

# Stop and remove volumes (WARNING: deletes database data)
docker-compose -f docker-compose-new.yml down -v
```

### View Logs
```bash
# All services
docker-compose -f docker-compose-new.yml logs -f

# Specific service
docker-compose -f docker-compose-new.yml logs -f core-service
docker-compose -f docker-compose-new.yml logs -f logiflow-service
docker-compose -f docker-compose-new.yml logs -f gateway-service
docker-compose -f docker-compose-new.yml logs -f postgres

# Last 100 lines
docker-compose -f docker-compose-new.yml logs --tail=100
```

### Access Container Shell
```bash
# Core Service
docker exec -it tenant-core-service /bin/sh

# LogiFlow Service
docker exec -it tenant-logiflow-service /bin/sh

# Gateway Service
docker exec -it tenant-gateway-service /bin/sh

# Database
docker exec -it tenant-platform-db psql -U neondb_owner -d neondb
```

### Inspect Services
```bash
# List running containers
docker ps

# Show container stats (CPU, memory, network)
docker stats

# Show container IP addresses
docker network inspect tenant-platform_tenant-platform
```

## Database Management

### Initial Setup

The database is automatically initialized:
1. PostgreSQL container starts
2. Database `neondb` is created
3. Flyway migrations run automatically (via core-service on first startup)

### Access Database

**From host machine:**
```bash
psql -h localhost -U neondb_owner -d neondb
```

**From within container:**
```bash
docker exec -it tenant-platform-db psql -U neondb_owner -d neondb
```

### View Migration History

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### Reset Database

⚠️ **WARNING:** This deletes all data!

```bash
# Option 1: Remove volume (clean start)
docker-compose -f docker-compose-new.yml down -v
docker-compose -f docker-compose-new.yml up

# Option 2: Manual delete in database
docker exec -it tenant-platform-db psql -U neondb_owner -d neondb -c "
  DROP SCHEMA public CASCADE;
  CREATE SCHEMA public;
"
```

## Troubleshooting

### Service Won't Start

**Check logs:**
```bash
docker-compose -f docker-compose-new.yml logs core-service
```

**Common issues:**
- **Port already in use**: Kill existing process or change port in `docker-compose-new.yml`
- **Database connection failed**: Wait for PostgreSQL health check to pass
- **Out of memory**: Increase Docker memory allocation

### Database Connection Refused

**Cause:** Core/LogiFlow services connecting before PostgreSQL is ready

**Solution:**
```bash
# Check postgres health
docker-compose -f docker-compose-new.yml ps

# Restart services
docker-compose -f docker-compose-new.yml restart core-service logiflow-service
```

### Flyway Migration Errors

**Cause:** Schema version conflicts or invalid SQL

**Check migration status:**
```bash
docker exec -it tenant-platform-db psql -U neondb_owner -d neondb -c \
  "SELECT * FROM flyway_schema_history;"
```

**Repair (if stuck):**
```bash
docker exec -it tenant-platform-db psql -U neondb_owner -d neondb -c \
  "DELETE FROM flyway_schema_history WHERE success = false;"
```

### Slow Build

**Multi-stage builds take time first run (5-10 min).**

Optimize:
```bash
# Use BuildKit for faster builds
DOCKER_BUILDKIT=1 docker-compose -f docker-compose-new.yml build --no-cache
```

## Testing with Postman

1. **Set base URL:**
   ```
   http://localhost:8080
   ```

2. **Login and get token:**
   ```
   POST /api/auth/login
   {
     "username": "admin",
     "password": "password",
     "tenantCode": "demo"
   }
   ```

3. **Use token in subsequent requests:**
   ```
   Header: Authorization: Bearer {accessToken}
   Header: X-Tenant-Code: demo
   ```

4. **Import collection:**
   Open `postman/tenantcore-gateway-mvp.postman_collection.json`

## Production Deployment

For production deployments (K8s, cloud providers):

### 1. Push Images to Registry
```bash
# Build and tag
docker-compose -f docker-compose-new.yml build
docker tag tenant-platform-core-service:latest myregistry.azurecr.io/tenant-core:v1.0.0
docker tag tenant-platform-logiflow-service:latest myregistry.azurecr.io/tenant-logiflow:v1.0.0
docker tag tenant-platform-gateway-service:latest myregistry.azurecr.io/tenant-gateway:v1.0.0

# Push
docker push myregistry.azurecr.io/tenant-core:v1.0.0
docker push myregistry.azurecr.io/tenant-logiflow:v1.0.0
docker push myregistry.azurecr.io/tenant-gateway:v1.0.0
```

### 2. Create Production .env
```env
POSTGRES_PASSWORD=very-secure-password-here
APP_JWT_SECRET=very-secure-jwt-secret-key
```

### 3. Use External PostgreSQL
```yaml
# Modify docker-compose-new.yml to remove postgres service
# and point services to external database
```

### 4. Scale Services (with Kubernetes)
```yaml
# core-service: 2 replicas
# logiflow-service: 3 replicas
# gateway-service: 2 replicas with load balancer
```

## Monitoring

### Container Resource Usage
```bash
docker stats
```

### Prometheus Metrics (optional)
Spring Boot Actuator exposes Prometheus metrics:
```
GET http://localhost:8081/actuator/prometheus
GET http://localhost:8082/actuator/prometheus
```

### Centralized Logging (optional)
Mount log volumes:
```yaml
volumes:
  - ./logs:/var/log/app
```

## Next Steps

1. ✅ Run `docker-compose -f docker-compose-new.yml up --build`
2. ✅ Verify health checks with `curl`
3. ✅ Test with Postman collection
4. ✅ Review logs: `docker-compose -f docker-compose-new.yml logs -f`
5. ✅ For production: update `.env` with secure secrets
6. ✅ Consider: Kubernetes deployment, CI/CD pipelines, monitoring stack

## Support

For issues or questions:
- Check service logs: `docker-compose logs -f <service-name>`
- Verify environment variables: `docker inspect <container-name>`
- Review migration history: Query `flyway_schema_history` table
