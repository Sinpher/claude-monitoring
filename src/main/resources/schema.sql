-- 仅作参考，实际由 JPA 自动建表
CREATE TABLE IF NOT EXISTS sessions (
    id VARCHAR(64) PRIMARY KEY,
    project VARCHAR(256),
    model VARCHAR(128),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    total_input_tokens BIGINT NOT NULL DEFAULT 0,
    total_output_tokens BIGINT NOT NULL DEFAULT 0,
    total_cache_tokens BIGINT NOT NULL DEFAULT 0,
    estimated_cost DECIMAL(10,6) DEFAULT 0,
    tool_call_count INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS tool_calls (
    id BIGINT PRIMARY KEY AUTOINCREMENT,
    session_id VARCHAR(64) NOT NULL,
    tool_name VARCHAR(64) NOT NULL,
    input_params TEXT,
    timestamp TIMESTAMP NOT NULL,
    duration INT
);

CREATE TABLE IF NOT EXISTS daily_usage (
    date DATE PRIMARY KEY,
    total_input_tokens BIGINT NOT NULL DEFAULT 0,
    total_output_tokens BIGINT NOT NULL DEFAULT 0,
    total_cache_tokens BIGINT NOT NULL DEFAULT 0,
    estimated_cost DECIMAL(10,6) DEFAULT 0,
    session_count INT NOT NULL DEFAULT 0,
    tool_call_count INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS agent_status (
    session_id VARCHAR(64) PRIMARY KEY,
    status VARCHAR(16) NOT NULL DEFAULT 'IDLE',
    current_tool VARCHAR(64),
    last_updated_at TIMESTAMP NOT NULL
);
