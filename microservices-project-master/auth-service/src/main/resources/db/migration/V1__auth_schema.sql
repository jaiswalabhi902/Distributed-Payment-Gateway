-- Auth service schema: users, roles, permissions and their join tables.

CREATE TABLE roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(200)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE permissions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(200)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE users (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    username           VARCHAR(50)  NOT NULL UNIQUE,
    email              VARCHAR(120) NOT NULL UNIQUE,
    password           VARCHAR(255) NOT NULL,
    enabled            BIT(1)       NOT NULL DEFAULT b'1',
    account_non_locked BIT(1)       NOT NULL DEFAULT b'1',
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_perms_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_perms_perm FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token       VARCHAR(512) NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL,
    expiry_date DATETIME(6)  NOT NULL,
    revoked     BIT(1)       NOT NULL DEFAULT b'0',
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_refresh_token ON refresh_tokens (token);

-- Reference data: roles
INSERT INTO roles (id, name, description) VALUES
    (1, 'ROLE_ADMIN',    'Full administrative access'),
    (2, 'ROLE_USER',     'Standard authenticated user'),
    (3, 'ROLE_MERCHANT', 'Merchant / vendor account');

-- Reference data: permissions
INSERT INTO permissions (id, name, description) VALUES
    (1, 'payment:read',    'View payments'),
    (2, 'payment:write',   'Create / update payments'),
    (3, 'payment:refund',  'Refund payments'),
    (4, 'settlement:read', 'View settlements'),
    (5, 'settlement:write','Manage settlements'),
    (6, 'user:read',       'View users'),
    (7, 'user:write',      'Manage users');

-- ROLE_ADMIN -> all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions;

-- ROLE_USER -> payment read/write
INSERT INTO role_permissions (role_id, permission_id) VALUES
    (2, 1),
    (2, 2);

-- ROLE_MERCHANT -> payment read, settlement read
INSERT INTO role_permissions (role_id, permission_id) VALUES
    (3, 1),
    (3, 4);
