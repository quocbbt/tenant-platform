# Docker Deployment - Stage 8 Setup

## 📦 What's Been Created

This Stage 8 setup provides a complete Docker-based deployment infrastructure for TenantCore + LogiFlow MVP.

### Files Created

1. **Dockerfiles** (Multi-stage builds)
   - `core-service/Dockerfile` - Core service container (IAM & Auth)
   - `logiflow-service/Dockerfile` - LogiFlow service container (Logistics)
   - `gateway-service/Dockerfile` - Gateway service container (API routing)

2. **Docker Compose Orchestration**
   - `docker-compose-new.yml` - Complete stack configuration
     - PostgreSQL database (5432)
     - Core Service (8081)
     - LogiFlow Service (8082)
     - Gateway Service (8080)
   - `.dockerignore` - Optimizes build context

3. **Configuration Updates**
   - `gateway-service/src/main/resources/application.yaml` - Updated with environment variables for Docker networking

4. **Helper Scripts**
   - `docker-helper.sh` - Bash script for Linux/Mac
   - `docker-helper.bat` - Batch script for Windows
   - Provides: build, start, stop, logs, health-check, db-access commands

5. **Documentation**
   - `docs/DOCKER_DEPLOYMENT_GUIDE.md` - Comprehensive deployment guide
   - `docs/BUSINESS_FEATURES_OUTLINE.md` - Business features reference

---

## 🚀 Quick Start

### Option 1: Using Helper Script (Recommended)

**Windows:**
```bash
docker-helper.bat start
```

**Linux/Mac:**
```bash
./docker-helper.sh start
```

**What it does:**
- ✅ Builds Docker images (first run only)
- ✅ Starts all services and PostgreSQL
- ✅ Waits for services to become healthy
- ✅ Displays service URLs

### Option 2: Manual Docker Compose

```bash
# Build images
docker-compose -f docker-compose-new.yml build --no-cache

# Start services
docker-compose -f docker-compose-new.yml up -d

# Check health
docker-compose -f docker-compose-new.yml ps
```

---

## 📋 Architecture

```
┌─────────────────────────────────────────┐
│    Gateway Service (8080)               │
│    Routes API requests                  │
├──────────┬──────────────────────────────┤
│          │                              │
│          ▼                              ▼
│   Core Service (8081)          LogiFlow Service (8082)
│   ├─ Login                      ├─ Orders
│   ├─ Auth                       ├─ Drivers
│   ├─ Users                      ├─ Vehicles
│   ├─ Roles                      ├─ Customers
│   ├─ Permissions                ├─ Reconciliation
│   └─ Flyway Migrations          └─ Operations
│          │                              │
└──────────┴──────────────────────────────┘
              ▼
       PostgreSQL (5432)
       └─ Shared Database
         - All tables
         - Multi-tenant isolation
         - Flyway migration history
```

---

## ✅ Verification Steps

### 1. Services Running
```bash
# Check all containers
docker ps

# Output should show 4 containers:
# - tenant-platform-db
# - tenant-core-service
# - tenant-logiflow-service
# - tenant-gateway-service
```

### 2. Health Checks
```bash
# Gateway health
curl http://localhost:8080/actuator/health

# Core service health
curl http://localhost:8081/actuator/health

# LogiFlow service health
curl http://localhost:8082/actuator/health

# Expected response: {"status":"UP"}
```

### 3. Database Connection
```bash
# Connect to database
docker exec -it tenant-platform-db psql -U neondb_owner -d neondb

# Check tables
\dt

# Check migrations
SELECT * FROM flyway_schema_history;
```

### 4. Test API
```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password",
    "tenantCode": "demo"
  }'

# Should return JWT token
```

---

## 🔧 Configuration

### Environment Variables

Create `.env` file in project root:

```env
# Database
POSTGRES_PASSWORD=postgres123

# JWT Secret (CHANGE IN PRODUCTION!)
APP_JWT_SECRET=your-secret-key-here

# Service URLs
CORE_SERVICE_URL=http://core-service:8081
LOGIFLOW_SERVICE_URL=http://logiflow-service:8082
```

### Service Ports

| Service | Port | URL |
|---------|------|-----|
| Gateway | 8080 | http://localhost:8080 |
| Core | 8081 | http://localhost:8081 |
| LogiFlow | 8082 | http://localhost:8082 |
| PostgreSQL | 5432 | localhost:5432 |

---

## 📝 Common Commands

### View Logs
```bash
# All services
docker-compose -f docker-compose-new.yml logs -f

# Specific service
docker-compose -f docker-compose-new.yml logs -f core-service
docker-compose -f docker-compose-new.yml logs -f logiflow-service
docker-compose -f docker-compose-new.yml logs -f gateway-service
```

### Access Shell
```bash
# Core service shell
docker exec -it tenant-core-service /bin/sh

# Database shell
docker exec -it tenant-platform-db psql -U neondb_owner -d neondb
```

### Stop/Restart
```bash
# Stop all
docker-compose -f docker-compose-new.yml stop

# Restart all
docker-compose -f docker-compose-new.yml restart

# Stop and remove (keeps data)
docker-compose -f docker-compose-new.yml down

# Stop and remove everything (DELETE DATA!)
docker-compose -f docker-compose-new.yml down -v
```

---

## 🛠️ Helper Script Commands

### Windows (docker-helper.bat)
```bash
docker-helper.bat build            # Build images
docker-helper.bat start            # Build and start
docker-helper.bat stop             # Stop services
docker-helper.bat restart          # Restart services
docker-helper.bat logs             # View all logs
docker-helper.bat logs core-service # View core-service logs
docker-helper.bat ps               # List containers
docker-helper.bat health           # Check service health
docker-helper.bat db-access        # Access database
docker-helper.bat clean            # Remove all (DATA LOSS!)
docker-helper.bat help             # Show help
```

### Linux/Mac (docker-helper.sh)
```bash
./docker-helper.sh build
./docker-helper.sh start
./docker-helper.sh stop
./docker-helper.sh logs [service]
./docker-helper.sh ps
./docker-helper.sh health
./docker-helper.sh db-access
./docker-helper.sh clean
```

---

## 🐛 Troubleshooting

### Service Won't Start

**Check logs:**
```bash
docker-compose -f docker-compose-new.yml logs core-service
```

**Common causes:**
- Port already in use: Kill existing process or change port in docker-compose-new.yml
- Database not ready: Wait 10-20 seconds, services have startup delays
- Out of memory: Increase Docker memory allocation

### Database Connection Error

**Cause:** Services connecting before PostgreSQL is ready

**Fix:**
```bash
# Wait for postgres to be healthy
docker-compose -f docker-compose-new.yml ps postgres

# Restart affected services
docker-compose -f docker-compose-new.yml restart core-service logiflow-service
```

### Flyway Migration Fails

**Check migration status:**
```bash
docker exec -it tenant-platform-db psql -U neondb_owner -d neondb -c \
  "SELECT * FROM flyway_schema_history;"
```

**Reset migrations (WARNING: deletes schema):**
```bash
docker exec -it tenant-platform-db psql -U neondb_owner -d neondb -c \
  "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

---

## 📚 Next Steps

1. ✅ **Run:** `docker-helper.bat start` (Windows) or `./docker-helper.sh start` (Linux/Mac)
2. ✅ **Verify:** `docker ps` to see all containers
3. ✅ **Check Health:** Run `docker-helper.bat health` or `./docker-helper.sh health`
4. ✅ **Test API:** Use Postman collection in `postman/tenantcore-gateway-mvp.postman_collection.json`
5. ✅ **Read Guide:** See `docs/DOCKER_DEPLOYMENT_GUIDE.md` for detailed information

---

## 🔒 Production Deployment

For production environments:

1. **Update .env with secure secrets:**
   ```env
   POSTGRES_PASSWORD=very-secure-password-here
   APP_JWT_SECRET=very-secure-jwt-secret-here
   ```

2. **Use external PostgreSQL database:**
   - Modify docker-compose-new.yml to remove postgres service
   - Point services to external DB with SPRING_DATASOURCE_URL

3. **Push images to container registry:**
   ```bash
   docker tag tenant-platform-core-service:latest myregistry.io/core:v1.0
   docker push myregistry.io/core:v1.0
   ```

4. **Deploy with Kubernetes/cloud provider:**
   - Create K8s YAML manifests
   - Use CI/CD pipeline for automated deployments
   - Consider: monitoring, logging, auto-scaling

---

## 📖 Documentation

- **Docker Deployment Guide:** `docs/DOCKER_DEPLOYMENT_GUIDE.md`
- **Business Features Outline:** `docs/BUSINESS_FEATURES_OUTLINE.md`
- **Implementation Guide:** `tenantcore_logiflow_mvp_guide_compact.md`

---

## ✨ What's Included

✅ **Multi-stage Dockerfiles** - Optimized image size (~200MB each)  
✅ **Docker Compose Stack** - PostgreSQL + 3 services  
✅ **Health Checks** - Automated service health monitoring  
✅ **Helper Scripts** - Windows and Linux/Mac convenience commands  
✅ **Comprehensive Guide** - Setup, troubleshooting, best practices  
✅ **Environment Configuration** - Secure secrets management  
✅ **Networking** - Docker bridge network for service-to-service communication  

---

## 🎯 Stage 8 Status

**✅ Docker deployment infrastructure complete:**
- Dockerfile for each service
- Docker Compose orchestration
- Helper scripts for all platforms
- Comprehensive deployment guide
- Configuration templates
- Health check setup
- Database networking

**Ready for:**
- Local development with Docker
- CI/CD pipeline integration
- Kubernetes deployment
- Production cloud deployment

---

**Questions or issues?** Check `docs/DOCKER_DEPLOYMENT_GUIDE.md` for detailed troubleshooting!
