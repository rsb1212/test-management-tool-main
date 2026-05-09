# Test Management Tool — Spring Boot REST API

## Overview

A production-grade Test Management System built with Spring Boot 3.2, PostgreSQL, Redis, and JWT authentication. Covers all modules from the DFD: User Management, Test Cases, Test Plans, Test Cycles, Test Runs, Executions, Defect Tracking (with Jira sync), and Reporting with export.

---

## Architecture

```
Frontend (React/Angular)
        │  HTTPS + JWT Bearer Token
        ▼
API Gateway / This Spring Boot App (port 8080)
   ├── Auth Service         → /api/v1/auth/**
   ├── User Service         → /api/v1/users/**
   ├── Test Case Service    → /api/v1/testcases/**
   ├── Test Plan Service    → /api/v1/testplans/**
   ├── Test Run Service     → /api/v1/testruns/**
   ├── Defect Service       → /api/v1/defects/**
   └── Reporting Service    → /api/v1/reports/**
        │
   ┌────┴────────────────────────┐
   ▼                             ▼
PostgreSQL (primary DB)      Redis (cache + sessions)
```

---

## Tech Stack

| Layer          | Technology                  |
|----------------|-----------------------------|
| Framework      | Spring Boot 3.2.4           |
| Language       | Java 17                     |
| Database       | PostgreSQL 15+              |
| Cache          | Redis 7+                    |
| Auth           | JWT (JJWT 0.12.3)           |
| ORM            | Spring Data JPA / Hibernate |
| Migrations     | Flyway                      |
| Rate Limiting  | Bucket4j                    |
| API Docs       | SpringDoc OpenAPI (Swagger) |
| PDF Export     | iTextPDF 5                  |
| Excel Export   | Apache POI 5                |
| Build Tool     | Maven 3.9+                  |

---

## Database Design

### Entity Relationship Summary

```
users
  ├── projects (owner_id)
  │     └── modules
  │     └── test_cases (project_id, module_id)
  │           └── test_steps
  │     └── test_plans (project_id)
  │           └── test_plan_cases [M:N with test_cases]
  │           └── test_cycles
  │                 └── test_runs (test_plan_id, test_cycle_id)
  │                       └── executions (test_run_id, test_case_id)
  │     └── defects (linked to test_run, test_case, execution)
  │           └── defect_comments
  └── attachments (polymorphic: entity_type + entity_id)
  └── audit_logs
```

### Key Tables

| Table           | Purpose                                        |
|-----------------|------------------------------------------------|
| users           | All users with roles: ADMIN/MANAGER/TESTER/VIEWER |
| projects        | Top-level project container with code (P-01)  |
| modules         | Logical grouping of test cases within a project |
| test_cases      | Test cases with priority, status, and steps   |
| test_steps      | Ordered steps inside each test case           |
| test_plans      | Plan containing a set of test cases           |
| test_plan_cases | M:N join between test_plans and test_cases    |
| test_cycles     | A cycle/iteration within a plan               |
| test_runs       | An actual run of a plan/cycle                 |
| executions      | Result of running each test case in a run     |
| defects         | Bugs raised, linked to executions/runs        |
| defect_comments | Comments thread on a defect                   |
| attachments     | File refs polymorphically linked to any entity|
| audit_logs      | JSONB-stored before/after change tracking     |

---

## Project Structure

```
src/main/java/com/testmgmt/
├── TestManagementApplication.java
├── config/
│   ├── SecurityConfig.java       ← Spring Security + JWT
│   ├── RedisConfig.java          ← Cache configuration
│   ├── OpenApiConfig.java        ← Swagger setup
│   └── RateLimitFilter.java      ← 100 req/min rate limiting
├── controller/
│   ├── Controllers.java          ← Auth, TestCase, TestRun, Defect, Report
│   ├── AdditionalControllers.java← User, Project, TestPlan
│   └── ExportController.java     ← PDF/Excel export
├── service/
│   ├── AuthService.java
│   ├── UserService.java
│   ├── UserDetailsServiceImpl.java
│   ├── TestCaseService.java
│   ├── TestPlanService.java
│   ├── TestRunService.java
│   ├── DefectService.java
│   ├── ReportingService.java
│   ├── ExportService.java
│   ├── JiraService.java
│   ├── AuditService.java
│   └── CodeSequenceService.java  ← TC-xxx, TR-xxx code generation
├── entity/
│   ├── BaseEntity.java
│   ├── User.java
│   ├── Project.java
│   ├── Module.java
│   ├── TestCase.java + TestStep.java
│   ├── TestPlan.java + TestCycle.java
│   ├── TestRun.java
│   ├── Execution.java
│   ├── Defect.java + DefectComment.java
│   └── AuditLog.java
├── repository/
│   └── Repositories.java         ← All Spring Data JPA repos
├── dto/request/
│   ├── AuthDTOs.java
│   └── RequestDTOs.java
├── exception/
│   └── GlobalExceptionHandler.java
└── security/
    ├── JwtUtil.java
    └── JwtAuthenticationFilter.java

src/main/resources/
├── application.properties
├── application-test.properties
└── db/migration/
    ├── V1__init_schema.sql       ← Full DB schema + seed admin
    └── V2__add_sequences.sql     ← Sequences for entity codes
```

---

## Prerequisites

Install the following before running:

| Tool        | Version  | Download                        |
|-------------|----------|---------------------------------|
| Java JDK    | 17+      | https://adoptium.net            |
| Maven       | 3.9+     | https://maven.apache.org        |
| PostgreSQL  | 15+      | https://www.postgresql.org      |
| Redis       | 7+       | https://redis.io/download       |
| Git         | any      | https://git-scm.com             |

---

## Local Setup — Step by Step

### Step 1: Create PostgreSQL Database

```sql
-- Connect as postgres superuser
psql -U postgres

CREATE DATABASE test_management_db;
CREATE USER testmgmt_user WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE test_management_db TO testmgmt_user;
\q
```

### Step 2: Start Redis

```bash
# On Linux/macOS
redis-server

# On Windows (use WSL or Docker)
docker run -d -p 6379:6379 redis:7-alpine
```

### Step 3: Configure the Application

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/test_management_db
spring.datasource.username=testmgmt_user
spring.datasource.password=yourpassword

spring.data.redis.host=localhost
spring.data.redis.port=6379

# Change this secret in production (must be Base64-encoded 256-bit key)
app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

# Jira (optional — only needed if using Jira sync)
app.jira.base-url=https://your-domain.atlassian.net
app.jira.username=your@email.com
app.jira.api-token=your-jira-api-token
app.jira.project-key=PROJ
```

### Step 4: Build and Run

```bash
# Clone or enter project directory
cd test-management

# Build (skipping tests for first run)
mvn clean package -DskipTests

# Run
java -jar target/test-management-tool-1.0.0.jar
```

Or run directly with Maven:

```bash
mvn spring-boot:run
```

### Step 5: Verify It's Running

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

Open Swagger UI: **http://localhost:8080/swagger-ui.html**

---

## Default Admin Credentials

The seed migration creates a default admin user:

| Field    | Value                |
|----------|----------------------|
| Email    | admin@testmgmt.com   |
| Password | Admin@123            |
| Role     | ADMIN                |

**Change this password immediately after first login.**

---

## API Quick Reference

### Authentication

```bash
# Login
POST /api/v1/auth/login
{
  "email": "admin@testmgmt.com",
  "password": "Admin@123"
}
# Returns: { "token": "JWT...", "expiresIn": 3600, "role": "ADMIN" }

# Register
POST /api/v1/auth/register
{
  "username": "rahul",
  "email": "rahul@company.com",
  "password": "SecurePass@123",
  "fullName": "Rahul Sharma",
  "team": "QA-Alpha"
}
```

### All Requests — Add JWT Header

```
Authorization: Bearer <your-token>
```

### Core Endpoints

```
# Projects
POST   /api/v1/projects
GET    /api/v1/projects
GET    /api/v1/projects/{id}

# Test Cases
POST   /api/v1/testcases
GET    /api/v1/testcases?projectId=&priority=HIGH&status=DRAFT&page=0&size=20
GET    /api/v1/testcases/{id}
PUT    /api/v1/testcases/{id}
DELETE /api/v1/testcases/{id}

# Test Plans
POST   /api/v1/testplans
GET    /api/v1/testplans?projectId=
POST   /api/v1/testplans/{id}/testcases
POST   /api/v1/testplans/{id}/cycles

# Test Runs
POST   /api/v1/testruns
PUT    /api/v1/testruns/{runId}/execute

# Defects
POST   /api/v1/defects
PATCH  /api/v1/defects/{id}/status
POST   /api/v1/defects/{id}/jira-sync
GET    /api/v1/defects?projectId=&severity=HIGH&status=NEW,OPEN

# Reports
GET    /api/v1/reports/execution-summary?projectId=&sprintId=&environment=UAT
GET    /api/v1/reports/defect-summary?projectId=
GET    /api/v1/reports/trends?projectId=&groupBy=week&dateFrom=&dateTo=
GET    /api/v1/reports/execution-summary/export?runId=&format=pdf
GET    /api/v1/reports/defect-summary/export?projectId=

# Users (Admin/Manager)
GET    /api/v1/users/profile
PUT    /api/v1/users/profile
GET    /api/v1/users               (Admin/Manager only)
PATCH  /api/v1/users/{id}/role     (Admin only)
```

---

## User Roles & Permissions

| Role    | Can Do                                              |
|---------|-----------------------------------------------------|
| ADMIN   | Everything including role management and deactivation |
| MANAGER | Create projects, plans, assign testers              |
| TESTER  | Create/update test cases, execute runs, raise defects |
| VIEWER  | Read-only access to all resources                   |

---

## Defect Status Workflow

```
NEW → OPEN → IN_PROGRESS → FIXED → RETEST → CLOSED
                         ↘               ↗
                         REJECTED   IN_PROGRESS (re-opened)
```

Invalid transitions return `422 Unprocessable Entity`.

---

## Running Tests

```bash
# All tests
mvn test

# With coverage report
mvn test jacoco:report
# Report: target/site/jacoco/index.html
```

---

## Docker Setup (Optional)

Create `docker-compose.yml` in the project root:

```yaml
version: '3.9'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: test_management_db
      POSTGRES_USER: testmgmt_user
      POSTGRES_PASSWORD: yourpassword
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/test_management_db
      SPRING_DATASOURCE_USERNAME: testmgmt_user
      SPRING_DATASOURCE_PASSWORD: yourpassword
      SPRING_DATA_REDIS_HOST: redis
      APP_JWT_SECRET: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
    depends_on:
      - postgres
      - redis

volumes:
  pgdata:
```

Create `Dockerfile` in project root:

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/test-management-tool-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Then run:

```bash
mvn clean package -DskipTests
docker-compose up -d
```

---

## Production Deployment Checklist

- [ ] Change `app.jwt.secret` to a new 256-bit Base64 key
- [ ] Change default admin password
- [ ] Set `spring.jpa.show-sql=false`
- [ ] Configure external Redis with password
- [ ] Configure S3 for attachments (`app.storage.type=s3`)
- [ ] Add HTTPS via reverse proxy (Nginx / AWS ALB)
- [ ] Set up log aggregation (ELK / CloudWatch)
- [ ] Configure Jira credentials for defect sync
- [ ] Set DB connection pool limits (`spring.datasource.hikari.maximum-pool-size=20`)
- [ ] Set up monitoring via `/actuator/health` and `/actuator/metrics`

---

## Generating a JWT Secret

```bash
# Linux/macOS
openssl rand -base64 32

# Java
import java.util.Base64;
import java.security.SecureRandom;
byte[] key = new byte[32];
new SecureRandom().nextBytes(key);
System.out.println(Base64.getEncoder().encodeToString(key));
```

---

## Common Errors & Fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `Connection refused 5432` | PostgreSQL not running | `sudo service postgresql start` |
| `Connection refused 6379` | Redis not running | `redis-server` or Docker |
| `401 Unauthorized` | Missing/expired JWT | Re-login to get new token |
| `422 Unprocessable Entity` | Invalid defect status transition | Check the status workflow above |
| `429 Too Many Requests` | Rate limit hit | Wait 60 seconds |
| `Flyway migration failed` | DB schema mismatch | `DROP DATABASE` and recreate |

---

## Support

Swagger UI: http://localhost:8080/swagger-ui.html
API Docs JSON: http://localhost:8080/api-docs
Health Check: http://localhost:8080/actuator/health
