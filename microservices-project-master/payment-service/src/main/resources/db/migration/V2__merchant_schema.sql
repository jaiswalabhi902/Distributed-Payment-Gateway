-- Merchant plane: merchant accounts, their orders, and capture attempts.

CREATE TABLE merchants (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    email          VARCHAR(150) NOT NULL UNIQUE,
    key_id         VARCHAR(60)  NOT NULL UNIQUE,
    key_secret_enc VARCHAR(255) NOT NULL,
    webhook_url    VARCHAR(500),
    active         BIT(1)       NOT NULL DEFAULT b'1',
    created_at     DATETIME(6)  NOT NULL,
    INDEX idx_merchants_key_id (key_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE merchant_orders (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_ref   VARCHAR(60)  NOT NULL UNIQUE,
    merchant_id BIGINT       NOT NULL,
    amount      BIGINT       NOT NULL,
    currency    VARCHAR(3)   NOT NULL,
    receipt     VARCHAR(150),
    status      VARCHAR(20)  NOT NULL,
    notes       TEXT,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    CONSTRAINT fk_order_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id),
    INDEX idx_orders_merchant (merchant_id),
    INDEX idx_orders_ref (order_ref)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE merchant_order_payments (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_ref VARCHAR(60)  NOT NULL UNIQUE,
    order_ref   VARCHAR(60)  NOT NULL,
    merchant_id BIGINT       NOT NULL,
    amount      BIGINT       NOT NULL,
    currency    VARCHAR(3)   NOT NULL,
    method      VARCHAR(30)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    INDEX idx_order_payments_order (order_ref),
    INDEX idx_order_payments_ref (payment_ref)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
