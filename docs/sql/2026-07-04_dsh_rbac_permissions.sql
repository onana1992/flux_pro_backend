-- Objectif : nouvelles permissions RBAC pour le module DSH (tableaux de bord et reporting)
-- Tables impactées : permissions (insertion), role_permissions (insertion)
-- Prérequis : docs/sql/2026-07-02_rbac_roles_permissions.sql déjà exécuté
-- Exécution : manuelle sur MySQL avant déploiement (ddl-auto=none)
-- Note : si RbacDataInitializer a déjà tourné au démarrage applicatif (transaction catchée en cas
-- d'échec), ce script est idempotent grâce aux clauses NOT EXISTS / INSERT IGNORE.

INSERT IGNORE INTO permissions (id, name, resource, action, description, created_at, updated_at)
SELECT UUID_TO_BIN(UUID()), v.name, v.resource, v.action, v.name, NOW(6), NOW(6)
FROM (
    SELECT 'DASHBOARD:READ' AS name, 'DASHBOARD' AS resource, 'READ' AS action
    UNION ALL SELECT 'DASHBOARD:EXPORT', 'DASHBOARD', 'EXPORT'
) v
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.name = v.name);

-- Lecture + export : SUPER_ADMIN, BUSINESS_ADMIN, DIRECTOR, SERVICE_HEAD, REGIONAL_DIRECTOR,
-- SECRETARY_GENERAL, EXECUTIVE_OFFICE (cf. SPEC-DSH.md §10.2)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN ('DASHBOARD:READ', 'DASHBOARD:EXPORT')
WHERE r.name IN ('SUPER_ADMIN', 'BUSINESS_ADMIN', 'DIRECTOR', 'SERVICE_HEAD', 'REGIONAL_DIRECTOR',
                  'SECRETARY_GENERAL', 'EXECUTIVE_OFFICE');

-- Lecture seule (activité personnelle) : AGENT, SUPPORT, READER
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'DASHBOARD:READ'
WHERE r.name IN ('AGENT', 'SUPPORT', 'READER');

-- rollback (commenté) :
-- DELETE FROM role_permissions WHERE permission_id IN (
--     SELECT id FROM permissions WHERE resource = 'DASHBOARD');
-- DELETE FROM permissions WHERE resource = 'DASHBOARD';
