CREATE TABLE IF NOT EXISTS storage_upload (
    id             UUID PRIMARY KEY,
    area           VARCHAR(30)  NOT NULL,
    object_key     VARCHAR(255) NOT NULL,
    owner_user_id  INTEGER      NOT NULL,
    reference      VARCHAR(64),
    kind           VARCHAR(20),
    original_name  VARCHAR(255),
    content_type   VARCHAR(100) NOT NULL,
    size_bytes     BIGINT       NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at   TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_storage_upload_key UNIQUE (object_key)
);

CREATE INDEX IF NOT EXISTS ix_storage_upload_pending ON storage_upload (status, expires_at);
CREATE INDEX IF NOT EXISTS ix_storage_upload_reference ON storage_upload (area, reference, status);

ALTER TABLE stock_import ADD COLUMN IF NOT EXISTS storage_key VARCHAR(255);
ALTER TABLE stock_import ADD COLUMN IF NOT EXISTS file_purged_at TIMESTAMP WITH TIME ZONE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_stock_import_storage_key ON stock_import (storage_key);
