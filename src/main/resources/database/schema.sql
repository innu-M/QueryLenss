CREATE TABLE IF NOT EXISTS database_connections (
    id INTEGER PRIMARY KEY,
    display_name TEXT NOT NULL,
    database_path TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS query_history (
    id INTEGER PRIMARY KEY,
    connection_id INTEGER,
    sql_text TEXT NOT NULL,
    query_type TEXT NOT NULL,
    duration_ms REAL,
    executed_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (connection_id) REFERENCES database_connections(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS saved_queries (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL UNIQUE,
    sql_text TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_saved_queries_title ON saved_queries(title);

CREATE TABLE IF NOT EXISTS query_analyses (
    id INTEGER PRIMARY KEY,
    history_id INTEGER NOT NULL UNIQUE,
    complexity_score INTEGER NOT NULL DEFAULT 0,
    risk_level TEXT NOT NULL,
    plan_text TEXT,
    FOREIGN KEY (history_id) REFERENCES query_history(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS recommendations (
    id INTEGER PRIMARY KEY,
    analysis_id INTEGER NOT NULL,
    message TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (analysis_id) REFERENCES query_analyses(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS comparison_sessions (
    id INTEGER PRIMARY KEY,
    title TEXT NOT NULL DEFAULT '',
    database_path TEXT NOT NULL,
    original_sql TEXT NOT NULL,
    ranking_strategy TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS comparison_candidates (
    id INTEGER PRIMARY KEY,
    comparison_id INTEGER NOT NULL,
    label TEXT NOT NULL,
    sql_text TEXT NOT NULL,
    median_ns INTEGER NOT NULL,
    p95_ns INTEGER NOT NULL,
    equivalent INTEGER NOT NULL CHECK (equivalent IN (0, 1)),
    status TEXT NOT NULL,
    rank_position INTEGER NOT NULL DEFAULT 0,
    explanation TEXT NOT NULL,
    plan_text TEXT NOT NULL,
    FOREIGN KEY (comparison_id) REFERENCES comparison_sessions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS benchmark_runs (
    id INTEGER PRIMARY KEY,
    candidate_id INTEGER NOT NULL,
    run_number INTEGER NOT NULL,
    duration_ns INTEGER NOT NULL CHECK (duration_ns >= 0),
    FOREIGN KEY (candidate_id) REFERENCES comparison_candidates(id) ON DELETE CASCADE,
    UNIQUE (candidate_id, run_number)
);

CREATE INDEX IF NOT EXISTS idx_comparison_sessions_created
    ON comparison_sessions(created_at);

CREATE INDEX IF NOT EXISTS idx_comparison_candidates_session
    ON comparison_candidates(comparison_id);
