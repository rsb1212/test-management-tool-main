-- ============================================================
-- Test Management System — Database Schema
-- V1__init_schema.sql
-- ============================================================

-- ─── Extensions ──────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── ENUM Types ──────────────────────────────────────────────
CREATE TYPE user_role       AS ENUM ('ADMIN','MANAGER','TESTER','VIEWER');
CREATE TYPE test_priority   AS ENUM ('CRITICAL','HIGH','MEDIUM','LOW');
CREATE TYPE test_status     AS ENUM ('DRAFT','READY','DEPRECATED');
CREATE TYPE run_status      AS ENUM ('NOT_STARTED','IN_PROGRESS','COMPLETED','ABORTED');
CREATE TYPE exec_result     AS ENUM ('PASSED','FAILED','BLOCKED','SKIPPED','IN_PROGRESS');
CREATE TYPE defect_severity AS ENUM ('CRITICAL','HIGH','MEDIUM','LOW');
CREATE TYPE defect_priority AS ENUM ('P1','P2','P3','P4');
CREATE TYPE defect_status   AS ENUM ('NEW','OPEN','IN_PROGRESS','FIXED','RETEST','CLOSED','REJECTED');

-- ─── users ───────────────────────────────────────────────────
CREATE TABLE users (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash TEXT         NOT NULL,
    full_name     VARCHAR(100),
    role          user_role    NOT NULL DEFAULT 'TESTER',
    team          VARCHAR(60),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role  ON users(role);

-- ─── projects ────────────────────────────────────────────────
CREATE TABLE projects (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(20)  NOT NULL UNIQUE,   -- e.g. P-01
    name        VARCHAR(120) NOT NULL,
    description TEXT,
    owner_id    UUID         REFERENCES users(id) ON DELETE SET NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_projects_owner ON projects(owner_id);

-- ─── modules (logical grouping inside projects) ───────────────
CREATE TABLE modules (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_modules_project ON modules(project_id);

-- ─── test_cases ──────────────────────────────────────────────
CREATE TABLE test_cases (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(20)  NOT NULL UNIQUE,   -- e.g. TC-205
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    preconditions    TEXT,
    project_id       UUID         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    module_id        UUID         REFERENCES modules(id) ON DELETE SET NULL,
    priority         test_priority NOT NULL DEFAULT 'MEDIUM',
    status           test_status   NOT NULL DEFAULT 'DRAFT',
    created_by       UUID         REFERENCES users(id) ON DELETE SET NULL,
    updated_by       UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tc_project  ON test_cases(project_id);
CREATE INDEX idx_tc_priority ON test_cases(priority);
CREATE INDEX idx_tc_status   ON test_cases(status);
CREATE INDEX idx_tc_module   ON test_cases(module_id);

-- ─── test_steps ──────────────────────────────────────────────
CREATE TABLE test_steps (
    id              UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    test_case_id    UUID    NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    step_number     INT     NOT NULL,
    action          TEXT    NOT NULL,
    expected_result TEXT    NOT NULL,
    UNIQUE (test_case_id, step_number)
);

CREATE INDEX idx_steps_tc ON test_steps(test_case_id);

-- ─── test_plans ──────────────────────────────────────────────
CREATE TABLE test_plans (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code         VARCHAR(20)  NOT NULL UNIQUE,  -- e.g. TP-10
    name         VARCHAR(200) NOT NULL,
    project_id   UUID         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    description  TEXT,
    sprint_id    VARCHAR(50),
    start_date   DATE,
    end_date     DATE,
    created_by   UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_plan_project ON test_plans(project_id);

-- ─── test_cycles (cycles inside a plan) ──────────────────────
CREATE TABLE test_cycles (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code         VARCHAR(20)  NOT NULL UNIQUE,  -- e.g. TC-05
    name         VARCHAR(200) NOT NULL,
    test_plan_id UUID         NOT NULL REFERENCES test_plans(id) ON DELETE CASCADE,
    environment  VARCHAR(60),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cycle_plan ON test_cycles(test_plan_id);

-- ─── test_plan_cases (M:N) ───────────────────────────────────
CREATE TABLE test_plan_cases (
    test_plan_id  UUID NOT NULL REFERENCES test_plans(id)  ON DELETE CASCADE,
    test_case_id  UUID NOT NULL REFERENCES test_cases(id)  ON DELETE CASCADE,
    PRIMARY KEY (test_plan_id, test_case_id)
);

-- ─── test_runs ───────────────────────────────────────────────
CREATE TABLE test_runs (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(20)  NOT NULL UNIQUE,   -- e.g. TR-88
    test_plan_id    UUID         NOT NULL REFERENCES test_plans(id) ON DELETE CASCADE,
    test_cycle_id   UUID         REFERENCES test_cycles(id) ON DELETE SET NULL,
    environment     VARCHAR(60),
    build_version   VARCHAR(40),
    assigned_to     UUID         REFERENCES users(id) ON DELETE SET NULL,
    status          run_status   NOT NULL DEFAULT 'NOT_STARTED',
    total_tests     INT          NOT NULL DEFAULT 0,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_by      UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_run_plan    ON test_runs(test_plan_id);
CREATE INDEX idx_run_status  ON test_runs(status);
CREATE INDEX idx_run_assignee ON test_runs(assigned_to);

-- ─── executions ──────────────────────────────────────────────
CREATE TABLE executions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(20)  NOT NULL UNIQUE,   -- e.g. EX-441
    test_run_id     UUID         NOT NULL REFERENCES test_runs(id) ON DELETE CASCADE,
    test_case_id    UUID         NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    result          exec_result  NOT NULL DEFAULT 'IN_PROGRESS',
    actual_result   TEXT,
    comment         TEXT,
    executed_by     UUID         REFERENCES users(id) ON DELETE SET NULL,
    executed_at     TIMESTAMPTZ,
    duration_secs   INT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (test_run_id, test_case_id)
);

CREATE INDEX idx_exec_run    ON executions(test_run_id);
CREATE INDEX idx_exec_tc     ON executions(test_case_id);
CREATE INDEX idx_exec_result ON executions(result);

-- ─── defects ─────────────────────────────────────────────────
CREATE TABLE defects (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(20)      NOT NULL UNIQUE,   -- e.g. DEF-1042
    title               VARCHAR(255)     NOT NULL,
    description         TEXT,
    severity            defect_severity  NOT NULL,
    priority            defect_priority  NOT NULL,
    status              defect_status    NOT NULL DEFAULT 'NEW',
    project_id          UUID             NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    linked_test_run_id  UUID             REFERENCES test_runs(id) ON DELETE SET NULL,
    linked_test_case_id UUID             REFERENCES test_cases(id) ON DELETE SET NULL,
    linked_execution_id UUID             REFERENCES executions(id) ON DELETE SET NULL,
    environment         VARCHAR(60),
    build_version       VARCHAR(40),
    module              VARCHAR(100),
    jira_issue_key      VARCHAR(50),
    jira_url            TEXT,
    reported_by         UUID             REFERENCES users(id) ON DELETE SET NULL,
    assigned_to         UUID             REFERENCES users(id) ON DELETE SET NULL,
    resolved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_defect_project  ON defects(project_id);
CREATE INDEX idx_defect_status   ON defects(status);
CREATE INDEX idx_defect_severity ON defects(severity);
CREATE INDEX idx_defect_run      ON defects(linked_test_run_id);
CREATE INDEX idx_defect_jira     ON defects(jira_issue_key);

-- ─── defect_comments ─────────────────────────────────────────
CREATE TABLE defect_comments (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    defect_id   UUID        NOT NULL REFERENCES defects(id) ON DELETE CASCADE,
    comment     TEXT        NOT NULL,
    created_by  UUID        REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dc_defect ON defect_comments(defect_id);

-- ─── attachments ─────────────────────────────────────────────
CREATE TABLE attachments (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type   VARCHAR(30) NOT NULL,   -- 'DEFECT','EXECUTION','TEST_CASE'
    entity_id     UUID        NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    file_path     TEXT        NOT NULL,
    file_size     BIGINT,
    content_type  VARCHAR(100),
    uploaded_by   UUID        REFERENCES users(id) ON DELETE SET NULL,
    uploaded_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attach_entity ON attachments(entity_type, entity_id);

-- ─── audit_logs ──────────────────────────────────────────────
CREATE TABLE audit_logs (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id   UUID        NOT NULL,
    action      VARCHAR(30) NOT NULL,   -- CREATE, UPDATE, DELETE, STATUS_CHANGE
    old_value   JSONB,
    new_value   JSONB,
    performed_by UUID       REFERENCES users(id) ON DELETE SET NULL,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_user   ON audit_logs(performed_by);

-- ─── Seed: default admin user (password: Admin@123) ──────────
INSERT INTO users (id, username, email, password_hash, full_name, role, team)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@testmgmt.com',
    '$2a$12$K8Y9z3mF1xvQ2wT5nJ7cMeGhLP0dRsA4uCbXiVpZ6eN3oI1jH9gOi',
    'System Admin',
    'ADMIN',
    'Platform'
);
