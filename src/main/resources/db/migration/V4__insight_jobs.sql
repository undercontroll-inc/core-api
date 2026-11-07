CREATE TABLE IF NOT EXISTS insight_job (
    id                 BIGSERIAL PRIMARY KEY,
    snapshot_id        BIGINT       NOT NULL,
    bucket_key         VARCHAR(10)  NOT NULL,
    prompt_version     VARCHAR(32)  NOT NULL,
    model_config_hash  VARCHAR(64)  NOT NULL,
    provider           VARCHAR(20),
    model              VARCHAR(100),
    status             VARCHAR(20)  NOT NULL,
    attempt_count      INTEGER      NOT NULL DEFAULT 0,
    lease_owner        VARCHAR(100),
    lease_until        TIMESTAMP WITH TIME ZONE,
    next_run_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error         TEXT,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at        TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_insight_job_snapshot_config UNIQUE (snapshot_id, prompt_version, model_config_hash)
);

CREATE INDEX IF NOT EXISTS ix_insight_job_runnable ON insight_job (status, next_run_at);
CREATE INDEX IF NOT EXISTS ix_insight_job_snapshot ON insight_job (snapshot_id, created_at DESC);

CREATE TABLE IF NOT EXISTS insight_attempt (
    id                 BIGSERIAL PRIMARY KEY,
    job_id             BIGINT       NOT NULL REFERENCES insight_job (id),
    attempt_number     INTEGER      NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    draft              TEXT,
    validation_errors  TEXT,
    error_kind         VARCHAR(30),
    error_message      TEXT,
    llm_calls          INTEGER      NOT NULL DEFAULT 0,
    started_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at        TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_insight_attempt_number UNIQUE (job_id, attempt_number)
);

CREATE TABLE IF NOT EXISTS market_insight_publication (
    id                     BIGSERIAL PRIMARY KEY,
    job_id                 BIGINT       NOT NULL REFERENCES insight_job (id),
    attempt_id             BIGINT       NOT NULL REFERENCES insight_attempt (id),
    snapshot_id            BIGINT       NOT NULL,
    bucket_key             VARCHAR(10)  NOT NULL,
    comparison_bucket_key  VARCHAR(10),
    payload                TEXT         NOT NULL,
    provider               VARCHAR(20),
    model                  VARCHAR(100),
    prompt_version         VARCHAR(32)  NOT NULL,
    published_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_market_insight_publication_job UNIQUE (job_id)
);

CREATE INDEX IF NOT EXISTS ix_market_insight_publication_published
    ON market_insight_publication (published_at DESC, id DESC);
