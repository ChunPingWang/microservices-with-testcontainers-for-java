CREATE TABLE stocks (
    sku        VARCHAR(64) PRIMARY KEY,
    available  INT NOT NULL,
    version    BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE reservations (
    sku       VARCHAR(64) NOT NULL REFERENCES stocks(sku) ON DELETE CASCADE,
    order_id  UUID NOT NULL,
    quantity  INT NOT NULL,
    PRIMARY KEY (sku, order_id)
);
