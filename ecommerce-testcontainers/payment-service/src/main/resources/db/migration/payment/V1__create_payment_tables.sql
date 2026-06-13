CREATE TABLE payments (
    id                 UUID PRIMARY KEY,
    order_id           UUID NOT NULL,
    buyer_id           VARCHAR(128) NOT NULL,
    idempotency_key    VARCHAR(128) NOT NULL UNIQUE,
    method             VARCHAR(32) NOT NULL,
    amount             NUMERIC(19, 4) NOT NULL,
    currency           VARCHAR(3) NOT NULL,
    status             VARCHAR(32) NOT NULL,
    status_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    auth_code          VARCHAR(64),
    receipt_uri        VARCHAR(2048),
    failure_reason     TEXT,
    version            BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_payments_order ON payments(order_id);
