-- Ensure code sequences continue after existing data.
-- Prevents duplicate-key errors after app restarts/migrations.

SELECT setval(
    'seq_tc',
    COALESCE((SELECT MAX(split_part(code, '-', 2)::bigint) FROM test_cases), 200) + 1,
    false
);

SELECT setval(
    'seq_tp',
    COALESCE((SELECT MAX(split_part(code, '-', 2)::bigint) FROM test_plans), 10) + 1,
    false
);

SELECT setval(
    'seq_tr',
    COALESCE((SELECT MAX(split_part(code, '-', 2)::bigint) FROM test_runs), 80) + 1,
    false
);

SELECT setval(
    'seq_ex',
    COALESCE((SELECT MAX(split_part(code, '-', 2)::bigint) FROM executions), 440) + 1,
    false
);

SELECT setval(
    'seq_def',
    COALESCE((SELECT MAX(split_part(code, '-', 2)::bigint) FROM defects), 1000) + 1,
    false
);

SELECT setval(
    'seq_cyc',
    COALESCE((SELECT MAX(split_part(code, '-', 2)::bigint) FROM test_cycles), 1) + 1,
    false
);
