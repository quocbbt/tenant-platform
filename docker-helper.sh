#!/bin/bash

# TenantCore + LogiFlow Docker Helper Script
# Usage: ./docker-helper.sh [command]
# Commands: build, start, stop, logs, clean, health-check, db-access

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$PROJECT_DIR/docker-compose-new.yml"
ENV_FILE="$PROJECT_DIR/.env"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
print_header() {
    echo -e "${BLUE}=== $1 ===${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    print_success "Docker and Docker Compose are installed"
}

build_images() {
    print_header "Building Docker Images"
    docker-compose -f "$COMPOSE_FILE" build --no-cache
    print_success "Docker images built successfully"
}

start_services() {
    print_header "Starting Services"
    
    if [ ! -f "$ENV_FILE" ]; then
        print_warning "No .env file found. Creating default .env file..."
        cat > "$ENV_FILE" << 'EOF'
# Database Configuration
POSTGRES_PASSWORD=postgres123

# JWT Configuration (Change in production!)
APP_JWT_SECRET=tenantcore-dev-secret-change-me-tenantcore-dev-secret

# Service URLs (for Docker networking)
CORE_SERVICE_URL=http://core-service:8081
LOGIFLOW_SERVICE_URL=http://logiflow-service:8082
EOF
        print_success "Created .env file with default values"
        print_warning "IMPORTANT: Update .env file with secure secrets before production use!"
    fi
    
    docker-compose -f "$COMPOSE_FILE" up -d
    print_success "Services started in background"
    
    echo ""
    print_header "Waiting for Services to Be Ready"
    sleep 5
    
    # Check services health
    check_service_health
}

stop_services() {
    print_header "Stopping Services"
    docker-compose -f "$COMPOSE_FILE" stop
    print_success "Services stopped"
}

restart_services() {
    print_header "Restarting Services"
    docker-compose -f "$COMPOSE_FILE" restart
    print_success "Services restarted"
}

view_logs() {
    local service=$1
    print_header "Viewing Logs${service:+ for $service}"
    
    if [ -z "$service" ]; then
        docker-compose -f "$COMPOSE_FILE" logs -f --tail=50
    else
        docker-compose -f "$COMPOSE_FILE" logs -f --tail=50 "$service"
    fi
}

check_service_health() {
    print_header "Checking Service Health"
    
    local services=("gateway-service:8080" "core-service:8081" "logiflow-service:8082")
    
    for service in "${services[@]}"; do
        IFS=':' read -r name port <<< "$service"
        
        if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1; then
            print_success "$name is healthy on port $port"
        else
            print_warning "$name is not yet ready on port $port (retrying...)"
            sleep 3
            if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1; then
                print_success "$name is healthy on port $port"
            else
                print_error "$name failed to become healthy"
            fi
        fi
    done
    
    echo ""
    print_success "All services are ready!"
    echo ""
    print_header "Service URLs"
    echo "Gateway (API Gateway): http://localhost:8080"
    echo "Core Service: http://localhost:8081"
    echo "LogiFlow Service: http://localhost:8082"
    echo "Database: localhost:5432"
}

list_containers() {
    print_header "Running Containers"
    docker-compose -f "$COMPOSE_FILE" ps
}

access_database() {
    print_header "Accessing PostgreSQL Database"
    print_warning "Connecting to database (type \\q to exit)..."
    docker exec -it tenant-platform-db psql -U neondb_owner -d neondb
}

clean_all() {
    print_header "Cleaning Up All Services and Data"
    print_warning "This will remove all containers and volumes (database data will be lost!)"
    read -p "Are you sure? (yes/no): " confirm
    
    if [ "$confirm" = "yes" ]; then
        docker-compose -f "$COMPOSE_FILE" down -v
        print_success "All services and data cleaned up"
    else
        print_warning "Clean up cancelled"
    fi
}

show_help() {
    cat << 'EOF'
TenantCore + LogiFlow Docker Helper

Usage: ./docker-helper.sh [command]

Commands:
    build               Build Docker images
    start               Build and start services
    stop                Stop running services
    restart             Restart services
    logs [service]      View logs (optional: specific service)
    ps                  List running containers
    health              Check service health
    db-access           Access PostgreSQL database
    clean               Stop and remove all containers/volumes (DATA LOSS!)
    help                Show this help message

Examples:
    ./docker-helper.sh build
    ./docker-helper.sh start
    ./docker-helper.sh logs core-service
    ./docker-helper.sh health
    ./docker-helper.sh clean

Service Names:
    - postgres
    - core-service
    - logiflow-service
    - gateway-service
EOF
}

# Main script
main() {
    check_docker
    
    command=${1:-help}
    
    case "$command" in
        build)
            build_images
            ;;
        start)
            build_images
            start_services
            ;;
        stop)
            stop_services
            ;;
        restart)
            restart_services
            ;;
        logs)
            view_logs "$2"
            ;;
        ps)
            list_containers
            ;;
        health)
            check_service_health
            ;;
        db-access)
            access_database
            ;;
        clean)
            clean_all
            ;;
        help|*)
            show_help
            ;;
    esac
}

main "$@"
