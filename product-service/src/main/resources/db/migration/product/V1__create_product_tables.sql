CREATE TABLE products (
    id              UUID PRIMARY KEY,
    sku             VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    price_amount    NUMERIC(19, 4) NOT NULL,
    price_currency  VARCHAR(3) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE orders (
    id             UUID PRIMARY KEY,
    buyer_id       VARCHAR(128) NOT NULL,
    total_amount   NUMERIC(19, 4) NOT NULL,
    total_currency VARCHAR(3) NOT NULL,
    status         VARCHAR(32) NOT NULL,
    payment_id     UUID,
    status_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    cancel_reason  TEXT,
    version        BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE order_lines (
    order_id         UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    line_index       INT NOT NULL,
    sku              VARCHAR(64) NOT NULL,
    quantity         INT NOT NULL,
    unit_price       NUMERIC(19, 4) NOT NULL,
    unit_currency    VARCHAR(3) NOT NULL,
    PRIMARY KEY (order_id, line_index)
);

CREATE INDEX idx_orders_buyer ON orders(buyer_id);
CREATE INDEX idx_orders_status ON orders(status);
