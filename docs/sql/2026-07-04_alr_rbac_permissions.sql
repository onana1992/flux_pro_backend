-- Objectif : nouvelles permissions RBAC pour le module ALR (types + règles d'alerte)
-- Tables impactées : permissions (insertion), role_permissions (insertion pour SUPER_ADMIN/BUSINESS_ADMIN
--   en écriture, et DIRECTOR/SERVICE_HEAD/REGIONAL_DIRECTOR/SECRETARY_GENERAL/EXECUTIVE_OFFICE en lecture)
-- Prérequis : docs/sql/2026-07-02_rbac_roles_permissions.sql déjà exécuté
-- Exécution : manuelle sur MySQL avant déploiement (ddl-auto=none)
-- Note : si RbacDataInitializer a déjà tourné au démarrage applicatif (transaction catchée en cas
-- d'échec), ce script est idempotent grâce aux clauses NOT EXISTS / INSERT IGNORE.

INSERT IGNORE INTO permissions (id, name, resource, action, description, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), v.name, v.resource, v.action, v.name, NOW(6), NOW(6)
FROM (
    SELECT 'ALERT_TYPES:READ' AS name, 'ALERT_TYPES' AS resource, 'READ' AS action
    UNION ALL SELECT 'ALERT_TYPES:CREATE', 'ALERT_TYPES', 'CREATE'
    UNION ALL SELECT 'ALERT_TYPES:UPDATE', 'ALERT_TYPES', 'UPDATE'
    UNION ALL SELECT 'ALERT_TYPES:DELETE', 'ALERT_TYPES', 'DELETE'
    UNION ALL SELECT 'ALERT_RULES:READ', 'ALERT_RULES', 'READ'
    UNION ALL SELECT 'ALERT_RULES:CREATE', 'ALERT_RULES', 'CREATE'
    UNION ALL SELECT 'ALERT_RULES:UPDATE', 'ALERT_RULES', 'UPDATE'
    UNION ALL SELECT 'ALERT_RULES:DELETE', 'ALERT_RULES', 'DELETE'
) v
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.name = v.name);

-- Accès complet : SUPER_ADMIN, BUSINESS_ADMIN
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'ALERT_TYPES:READ', 'ALERT_TYPES:CREATE', 'ALERT_TYPES:UPDATE', 'ALERT_TYPES:DELETE',
    'ALERT_RULES:READ', 'ALERT_RULES:CREATE', 'ALERT_RULES:UPDATE', 'ALERT_RULES:DELETE')
WHERE r.name IN ('SUPER_ADMIN', 'BUSINESS_ADMIN');

-- Lecture seule : rôles destinataires/consultation des alertes
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('ALERT_TYPES:READ', 'ALERT_RULES:READ')
WHERE r.name IN ('DIRECTOR', 'SERVICE_HEAD', 'REGIONAL_DIRECTOR', 'SECRETARY_GENERAL', 'EXECUTIVE_OFFICE');

-- rollback (commenté) :
-- DELETE FROM role_permissions WHERE permission_id IN (
--     SELECT id FROM permissions WHERE resource IN ('ALERT_TYPES', 'ALERT_RULES'));
-- DELETE FROM permissions WHERE resource IN ('ALERT_TYPES', 'ALERT_RULES');
