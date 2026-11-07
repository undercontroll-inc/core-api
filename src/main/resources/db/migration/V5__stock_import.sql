CREATE TABLE IF NOT EXISTS stock_import (
    id                      BIGSERIAL PRIMARY KEY,
    created_by              INTEGER,
    status                  VARCHAR(20)  NOT NULL,
    stage                   VARCHAR(20),
    source_type             VARCHAR(20)  NOT NULL,
    file_name               VARCHAR(255),
    mime_type               VARCHAR(50)  NOT NULL,
    file_sha256             VARCHAR(64)  NOT NULL,
    failure_reason          VARCHAR(30),
    failure_message         VARCHAR(500),
    supplier_name           VARCHAR(255),
    supplier_cnpj           VARCHAR(20),
    invoice_number          VARCHAR(60),
    invoice_series          VARCHAR(20),
    issued_at               TIMESTAMP WITH TIME ZONE,
    access_key              VARCHAR(44),
    total                   NUMERIC(14, 2),
    warnings                VARCHAR(255),
    duplicate_of_import_id  BIGINT,
    item_count              INTEGER      NOT NULL DEFAULT 0,
    attempt_count           INTEGER      NOT NULL DEFAULT 0,
    lease_owner             VARCHAR(100),
    lease_until             TIMESTAMP WITH TIME ZONE,
    next_run_at             TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_at            TIMESTAMP WITH TIME ZONE,
    finished_at             TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS ix_stock_import_runnable ON stock_import (status, next_run_at);
CREATE INDEX IF NOT EXISTS ix_stock_import_created ON stock_import (created_at);
CREATE INDEX IF NOT EXISTS ix_stock_import_access_key ON stock_import (access_key);

CREATE TABLE IF NOT EXISTS stock_import_item (
    id                        BIGSERIAL PRIMARY KEY,
    import_id                 BIGINT        NOT NULL REFERENCES stock_import (id) ON DELETE CASCADE,
    line_number               INTEGER       NOT NULL,
    supplier_code             VARCHAR(60),
    description               VARCHAR(255)  NOT NULL,
    quantity                  NUMERIC(18, 4) NOT NULL,
    unit                      VARCHAR(20),
    unit_price                NUMERIC(18, 4),
    total                     NUMERIC(14, 2),
    confidence                DOUBLE PRECISION NOT NULL,
    warnings                  VARCHAR(255),
    match_type                VARCHAR(20)   NOT NULL,
    matched_component_id      INTEGER,
    match_confidence          DOUBLE PRECISION NOT NULL,
    candidates                TEXT,
    action                    VARCHAR(20)   NOT NULL,
    component_id              INTEGER,
    update_price              BOOLEAN       NOT NULL DEFAULT FALSE,
    new_name                  VARCHAR(255),
    new_brand                 VARCHAR(255),
    new_category              VARCHAR(255),
    new_description           VARCHAR(255),
    result_component_id       INTEGER,
    result_created            BOOLEAN,
    result_previous_quantity  BIGINT,
    result_new_quantity       BIGINT
);

CREATE INDEX IF NOT EXISTS ix_stock_import_item_import ON stock_import_item (import_id);

CREATE TABLE IF NOT EXISTS stock_import_file (
    import_id  BIGINT PRIMARY KEY REFERENCES stock_import (id) ON DELETE CASCADE,
    content    BYTEA  NOT NULL
);

CREATE TABLE IF NOT EXISTS component_supplier_alias (
    id            BIGSERIAL PRIMARY KEY,
    supplier_key  VARCHAR(255) NOT NULL,
    product_key   VARCHAR(255) NOT NULL,
    component_id  INTEGER      NOT NULL REFERENCES component (id) ON DELETE CASCADE,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_component_supplier_alias UNIQUE (supplier_key, product_key)
);
