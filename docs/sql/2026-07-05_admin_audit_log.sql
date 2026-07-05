-- Objectif : journal d'audit générique des actions d'administration (création, modification,
--            exécution), distinct de login_audit qui ne trace que les connexions.
-- Tables impactées : admin_audit_log (création), permissions (ajout de AUDIT_LOG:READ)
-- Prérequis : schéma users/permissions existant (id en BINARY(16) — UUID Hibernate)
-- Exécution : manuelle sur MySQL avant déploiement (ddl-auto=none)
--
-- Alimentation : AdminAuditAspect (AOP) journalise automatiquement tout endpoint annoté
-- @RequiresPermission dont l'action n'est ni READ ni EXPORT (ex: ROLES:CREATE, FILES:TRANSMIT,
-- USERS:RESET_PASSWORD...). Lecture exposée via GET /api/admin/audit-log (permission AUDIT_LOG:READ).
--
-- Note : l'insertion de la permission AUDIT_LOG:READ dans la table `permissions` est également
-- effectuée automatiquement au démarrage par RbacDataInitializer (seedPermissions/seedRoles) ;
-- l'INSERT ci-dessous est fourni à titre de secours si l'initialisation applicative est ignorée.
--
-- UUID : UNHEX(REPLACE(UUID(), '-', '')) — compatible MariaDB 10.4 (pas de UUID_TO_BIN).

CREATE TABLE IF NOT EXISTS admin_audit_log (
    id              BINARY(16)   NOT NULL PRIMARY KEY,
    actor_user_id   BINARY(16)   NULL,
    actor_email     VARCHAR(255) NOT NULL,
    resource_type   VARCHAR(64)  NOT NULL,
    action          VARCHAR(32)  NOT NULL,
    resource_id     VARCHAR(64)  NULL,
    resource_label  VARCHAR(255) NULL,
    success         BOOLEAN      NOT NULL,
    error_message   VARCHAR(500) NULL,
    ip_address      VARCHAR(45)  NULL,
    user_agent      TEXT         NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_admin_audit_log_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_admin_audit_log_created (created_at),
    INDEX idx_admin_audit_log_resource (resource_type, action),
    INDEX idx_admin_audit_log_actor (actor_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO permissions (id, name, resource, action, description, created_at, updated_at)
SELECT UNHEX(REPLACE(UUID(), '-', '')), 'AUDIT_LOG:READ', 'AUDIT_LOG', 'READ', 'AUDIT_LOG:READ',
       CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'AUDIT_LOG:READ');

-- rollback (commenté) :
-- DELETE FROM permissions WHERE name = 'AUDIT_LOG:READ';
-- DROP TABLE IF EXISTS admin_audit_log;
