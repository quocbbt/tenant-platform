@echo off
REM TenantCore + LogiFlow Docker Helper Script (Windows)
REM Usage: docker-helper.bat [command]
REM Commands: build, start, stop, logs, clean, health-check, db-access

setlocal enabledelayedexpansion

set COMPOSE_FILE=docker-compose-new.yml
set ENV_FILE=.env

REM Colors (requires Windows 10+ or ANSI enabled)
set GREEN=[92m
set RED=[91m
set YELLOW=[93m
set BLUE=[94m
set NC=[0m

echo.
echo ============================================
echo TenantCore + LogiFlow Docker Helper
echo ============================================
echo.

REM Check if command is provided
if "%1"=="" (
    call :show_help
    exit /b 0
)

REM Route commands
if /i "%1"=="build" (
    call :build_images
) else if /i "%1"=="start" (
    call :start_services
) else if /i "%1"=="stop" (
    call :stop_services
) else if /i "%1"=="restart" (
    call :restart_services
) else if /i "%1"=="logs" (
    call :view_logs %2
) else if /i "%1"=="ps" (
    call :list_containers
) else if /i "%1"=="health" (
    call :check_health
) else if /i "%1"=="db-access" (
    call :access_database
) else if /i "%1"=="clean" (
    call :clean_all
) else if /i "%1"=="help" (
    call :show_help
) else (
    echo Unknown command: %1
    call :show_help
    exit /b 1
)

goto :eof

REM ============================================
REM Functions
REM ============================================

:build_images
echo [Building Docker Images...]
docker-compose -f %COMPOSE_FILE% build --no-cache
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Docker images built successfully
) else (
    echo [ERROR] Failed to build images
    exit /b 1
)
exit /b 0

:start_services
echo [Building and Starting Services...]

REM Check if .env exists
if not exist %ENV_FILE% (
    echo [WARNING] No .env file found. Creating default .env file...
    (
        echo # Database Configuration
        echo POSTGRES_PASSWORD=postgres123
        echo.
        echo # JWT Configuration ^(Change in production!^)
        echo APP_JWT_SECRET=tenantcore-dev-secret-change-me-tenantcore-dev-secret
        echo.
        echo # Service URLs ^(for Docker networking^)
        echo CORE_SERVICE_URL=http://core-service:8081
        echo LOGIFLOW_SERVICE_URL=http://logiflow-service:8082
    ) > %ENV_FILE%
    echo [SUCCESS] Created .env file with default values
    echo [WARNING] IMPORTANT: Update .env file with secure secrets before production!
    echo.
)

docker-compose -f %COMPOSE_FILE% up -d
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to start services
    exit /b 1
)

echo [SUCCESS] Services started in background
echo.
echo [Waiting for services to be ready (10 seconds)...]
timeout /t 10 /nobreak
echo.

call :check_health
exit /b 0

:stop_services
echo [Stopping Services...]
docker-compose -f %COMPOSE_FILE% stop
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Services stopped
) else (
    echo [ERROR] Failed to stop services
    exit /b 1
)
exit /b 0

:restart_services
echo [Restarting Services...]
docker-compose -f %COMPOSE_FILE% restart
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Services restarted
) else (
    echo [ERROR] Failed to restart services
    exit /b 1
)
exit /b 0

:view_logs
if "%~1"=="" (
    echo [Viewing All Logs...]
    docker-compose -f %COMPOSE_FILE% logs -f --tail=50
) else (
    echo [Viewing %~1 Logs...]
    docker-compose -f %COMPOSE_FILE% logs -f --tail=50 %~1
)
exit /b 0

:list_containers
echo [Running Containers...]
docker-compose -f %COMPOSE_FILE% ps
exit /b 0

:check_health
echo [Checking Service Health...]
echo.

REM Check each service
for %%S in (gateway-service:8080 core-service:8081 logiflow-service:8082) do (
    for /f "tokens=1,2 delims=:" %%A in ("%%S") do (
        set SERVICE=%%A
        set PORT=%%B
        echo Checking !SERVICE! on port !PORT!...
        
        REM Try to curl health endpoint
        curl -s http://localhost:!PORT!/actuator/health > nul 2>&1
        if !ERRORLEVEL! EQU 0 (
            echo   [OK] !SERVICE! is healthy
        ) else (
            echo   [WAIT] !SERVICE! not ready, retrying...
            timeout /t 3 /nobreak > nul
            
            curl -s http://localhost:!PORT!/actuator/health > nul 2>&1
            if !ERRORLEVEL! EQU 0 (
                echo   [OK] !SERVICE! is healthy
            ) else (
                echo   [ERROR] !SERVICE! failed to become healthy
            )
        )
    )
)

echo.
echo [SUCCESS] All services are ready!
echo.
echo Service URLs:
echo   Gateway: http://localhost:8080
echo   Core Service: http://localhost:8081
echo   LogiFlow Service: http://localhost:8082
echo   Database: localhost:5432
echo.
exit /b 0

:access_database
echo [Connecting to PostgreSQL Database...]
echo Type \q to exit...
echo.
docker exec -it tenant-platform-db psql -U neondb_owner -d neondb
exit /b 0

:clean_all
echo [WARNING] This will remove all containers and volumes!
echo [WARNING] Database data will be LOST!
echo.
set /p confirm="Are you sure? (yes/no): "
if /i "%confirm%"=="yes" (
    docker-compose -f %COMPOSE_FILE% down -v
    echo [SUCCESS] All services and data cleaned up
) else (
    echo [CANCELLED] Clean up cancelled
)
exit /b 0

:show_help
echo TenantCore + LogiFlow Docker Helper
echo.
echo Usage: docker-helper.bat [command]
echo.
echo Commands:
echo   build               Build Docker images
echo   start               Build and start services
echo   stop                Stop running services
echo   restart             Restart services
echo   logs [service]      View logs (optional: specific service)
echo   ps                  List running containers
echo   health              Check service health
echo   db-access           Access PostgreSQL database
echo   clean               Stop and remove containers/volumes (DATA LOSS!)
echo   help                Show this help message
echo.
echo Examples:
echo   docker-helper.bat build
echo   docker-helper.bat start
echo   docker-helper.bat logs core-service
echo   docker-helper.bat health
echo   docker-helper.bat clean
echo.
echo Service Names:
echo   - postgres
echo   - core-service
echo   - logiflow-service
echo   - gateway-service
echo.
exit /b 0
