-- Convert PostgreSQL enum columns to VARCHAR so JPA EnumType.STRING
-- binds safely across drivers/environments.

ALTER TABLE users
    ALTER COLUMN role TYPE VARCHAR(20) USING role::text;

ALTER TABLE test_cases
    ALTER COLUMN priority TYPE VARCHAR(20) USING priority::text,
    ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

ALTER TABLE test_runs
    ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

ALTER TABLE executions
    ALTER COLUMN result TYPE VARCHAR(20) USING result::text;

ALTER TABLE defects
    ALTER COLUMN severity TYPE VARCHAR(20) USING severity::text,
    ALTER COLUMN priority TYPE VARCHAR(20) USING priority::text,
    ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
