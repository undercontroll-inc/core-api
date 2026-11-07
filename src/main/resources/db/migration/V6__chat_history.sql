CREATE TABLE IF NOT EXISTS chat_conversation (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               INTEGER      NOT NULL,
    public_id             VARCHAR(64)  NOT NULL,
    title                 VARCHAR(120),
    title_source          VARCHAR(10)  NOT NULL,
    last_message_preview  VARCHAR(160),
    message_count         INTEGER      NOT NULL DEFAULT 0,
    last_seq              INTEGER      NOT NULL DEFAULT 0,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    last_message_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at            TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_chat_conversation_user_public UNIQUE (user_id, public_id)
);

CREATE INDEX IF NOT EXISTS ix_chat_conversation_recent
    ON chat_conversation (user_id, last_message_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_chat_conversation_deleted
    ON chat_conversation (deleted_at)
    WHERE deleted_at IS NOT NULL;

CREATE TABLE IF NOT EXISTS chat_message (
    id               BIGSERIAL PRIMARY KEY,
    conversation_id  BIGINT       NOT NULL REFERENCES chat_conversation (id) ON DELETE CASCADE,
    seq              INTEGER      NOT NULL,
    role             VARCHAR(10)  NOT NULL,
    content          TEXT         NOT NULL,
    state            VARCHAR(10)  NOT NULL,
    reason           VARCHAR(40),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_chat_message_seq UNIQUE (conversation_id, seq)
);
