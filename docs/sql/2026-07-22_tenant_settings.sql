-- Objectif : configuration unique du déploiement (tenant) — fuseau, pays, branding, from email
-- Tables impactées : tenant_settings (création)
-- Exécution : manuelle sur MySQL (ddl-auto=none)
-- Seed : aussi appliqué au démarrage si table vide (TenantSettingsService)

CREATE TABLE IF NOT EXISTS tenant_settings (
    id                  BINARY(16)   NOT NULL PRIMARY KEY,
    tenant_name         VARCHAR(150) NOT NULL,
    product_name        VARCHAR(80)  NOT NULL,
    timezone            VARCHAR(64)  NOT NULL,
    country_code        VARCHAR(2)   NOT NULL,
    reference_prefix    VARCHAR(20)  NOT NULL,
    badge               VARCHAR(200) NOT NULL,
    from_address        VARCHAR(200) NOT NULL,
    email_redirect_to   VARCHAR(200) NULL,
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
