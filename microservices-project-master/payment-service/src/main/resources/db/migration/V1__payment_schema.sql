-- Payment service schema: payments and their refunds.

CREATE TABLE payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        VARCHAR(100)   NOT NULL UNIQUE,
    user_id         BIGINT         NOT NULL,
    amount          DECIMAL(18, 2) NOT NULL,
    currency        VARCHAR(3)     NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    payment_method  VARCHAR(30)    NOT NULL,
    transaction_id  VARCHAR(100)   NOT NULL UNIQUE,
    description     VARCHAR(500),
    refunded_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    version         BIGINT         NOT NULL DEFAULT 0,
    created_at      DATETIME(6)    NOT NULL,
    updated_at      DATETIME(6)    NOT NULL,
    INDEX idx_payments_user_id (user_id),
    INDEX idx_payments_status (status),
    INDEX idx_payments_order_id (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE payment_refunds (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id    BIGINT         NOT NULL,
    refund_amount DECIMAL(18, 2) NOT NULL,
    reason        VARCHAR(500),
    status        VARCHAR(20)    NOT NULL,
    created_at    DATETIME(6)    NOT NULL,
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE,
    INDEX idx_refunds_payment_id (payment_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
