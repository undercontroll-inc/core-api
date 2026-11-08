ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS outcome            VARCHAR(20);
ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS deadline           DATE;
ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS completed_at       TIMESTAMP WITH TIME ZONE;
ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS delivered_at       TIMESTAMP WITH TIME ZONE;
ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS tracking_token     VARCHAR(32);
ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS budget_sent_at     TIMESTAMP WITH TIME ZONE;
ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS budget_labor_total DOUBLE PRECISION;
ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS budget_parts_total DOUBLE PRECISION;
ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS budget_discount    DOUBLE PRECISION;
ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS budget_total       DOUBLE PRECISION;
ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS approval_decision  VARCHAR(20);
ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS decided_at         TIMESTAMP WITH TIME ZONE;
ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS decided_via        VARCHAR(20);
ALTER TABLE "orders" ADD COLUMN IF NOT EXISTS version            BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_tracking_token ON "orders" (tracking_token);

UPDATE "orders" SET deadline = completed_time WHERE deadline IS NULL AND completed_time IS NOT NULL;
UPDATE "orders" SET status = 'RECEIVED' WHERE status IN ('PENDING', 'IN_ANALYSIS');
UPDATE "orders"
   SET status = 'READY_FOR_PICKUP', outcome = 'REPAIRED', completed_at = COALESCE(completed_at, updated_at)
 WHERE status = 'COMPLETED';
UPDATE "orders"
   SET outcome = 'REPAIRED',
       completed_at = COALESCE(completed_at, updated_at),
       delivered_at = COALESCE(delivered_at, updated_at)
 WHERE status = 'DELIVERED' AND outcome IS NULL;

CREATE TABLE IF NOT EXISTS order_status_event (
    id           BIGSERIAL PRIMARY KEY,
    order_id     INTEGER      NOT NULL REFERENCES "orders" (id) ON DELETE CASCADE,
    from_status  VARCHAR(30),
    to_status    VARCHAR(30)  NOT NULL,
    outcome      VARCHAR(20),
    actor        VARCHAR(20)  NOT NULL,
    note         VARCHAR(500),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_order_status_event_order ON order_status_event (order_id, created_at);

CREATE TABLE IF NOT EXISTS order_photo (
    id            BIGSERIAL PRIMARY KEY,
    order_id      INTEGER      NOT NULL REFERENCES "orders" (id) ON DELETE CASCADE,
    kind          VARCHAR(10)  NOT NULL,
    storage_key   VARCHAR(255) NOT NULL,
    content_type  VARCHAR(50)  NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_order_photo_order ON order_photo (order_id, created_at);
