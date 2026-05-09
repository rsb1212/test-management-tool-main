#!/bin/bash

# ============================================================================
# Quick Docker Compose Startup - for testing/development
# ============================================================================

echo "Starting PostgreSQL, Redis, and Test Management App..."
echo ""

# Navigate to current directory
cd "
$(dirname "$0")" || exit 1

# Stop any existing containers
echo "Stopping existing containers..."
docker-compose down 2>/dev/null || true

echo ""
echo "Building and starting services..."
docker-compose up -d

echo ""
echo "Waiting for services to be healthy..."
sleep 30

echo ""
echo "============================================================================"
echo "All services are running!"
echo "============================================================================"
echo ""
echo "Access the application:"
echo "  - Swagger UI: http://localhost:8080/swagger-ui.html"
echo "  - Health:     http://localhost:8080/actuator/health"
echo "  - API Docs:   http://localhost:8080/api-docs"
echo ""
echo "Database Credentials:"
echo "  - Host:     localhost:5432"
echo "  - Database: test_management_db"
echo "  - User:     testmgmt_user"
echo "  - Password: yourpassword"
echo ""
echo "Redis:"
echo "  - Host: localhost:6379"
echo ""
echo "Admin Login:"
echo "  - Email: admin@testmgmt.com"
echo "  - Password: Admin@123"
echo ""
echo "To stop services: docker-compose down"
echo "To view logs: docker-compose logs -f app"
echo ""