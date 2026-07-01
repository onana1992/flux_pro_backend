-- FluxPro — Schéma identité & organisation (USR / RBAC)
-- Exécution manuelle sur MySQL avant démarrage applicatif (ddl-auto=none)

CREATE TABLE IF NOT EXISTS organization (
    id          CHAR(36)     NOT NULL PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(30)  NOT NULL,
    parent_id   CHAR(36)     NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_organization_parent FOREIGN KEY (parent_id) REFERENCES organization(id)
);

CREATE INDEX IF NOT EXISTS idx_organization_parent ON organization(parent_id);
CREATE INDEX IF NOT EXISTS idx_organization_code ON organization(code);

CREATE TABLE IF NOT EXISTS users (
    id                    CHAR(36)     NOT NULL PRIMARY KEY,
    staff_number          VARCHAR(32)  NOT NULL UNIQUE,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    last_name             VARCHAR(100) NOT NULL,
    first_name            VARCHAR(100) NOT NULL,
    phone                 VARCHAR(20)  NULL,
    role                  VARCHAR(30)  NOT NULL,
    organization_id       CHAR(36)     NOT NULL,
    job_title             VARCHAR(255) NULL,
    password_hash         VARCHAR(255) NOT NULL,
    must_change_password  BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          DATETIME(6)  NULL,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    substitute_id         CHAR(36)     NULL,
    created_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_users_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_users_substitute FOREIGN KEY (substitute_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_users_organization ON users(organization_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

CREATE TABLE IF NOT EXISTS login_audit (
    id              CHAR(36)     NOT NULL PRIMARY KEY,
    user_id         CHAR(36)     NULL,
    email           VARCHAR(255) NOT NULL,
    success         BOOLEAN      NOT NULL,
    ip_address      VARCHAR(45)  NULL,
    user_agent      TEXT         NULL,
    failure_reason  VARCHAR(50)  NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_login_audit_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_login_audit_created ON login_audit(created_at);

CREATE TABLE IF NOT EXISTS refresh_token (
    id          CHAR(36)     NOT NULL PRIMARY KEY,
    token       VARCHAR(512) NOT NULL UNIQUE,
    user_id     CHAR(36)     NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_token(user_id);
