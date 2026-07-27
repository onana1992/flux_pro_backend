-- Objectif : aligner la table roles sur l'enum UserRole Java
-- Prérequis : table roles (RBAC)
-- Idempotent

INSERT INTO roles (id, name, description, created_at, updated_at)
SELECT UNHEX(REPLACE(UUID(), '-', '')), 'SUPER_ADMIN', 'Administrateur système', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'SUPER_ADMIN');

INSERT INTO roles (id, name, description, created_at, updated_at)
SELECT UNHEX(REPLACE(UUID(), '-', '')), 'BUSINESS_ADMIN', 'Administrateur métier', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'BUSINESS_ADMIN');

INSERT INTO roles (id, name, description, created_at, updated_at)
SELECT UNHEX(REPLACE(UUID(), '-', '')), 'EXECUTIVE_OFFICE', 'Cabinet / Ministre', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'EXECUTIVE_OFFICE');

INSERT INTO roles (id, name, description, created_at, updated_at)
SELECT UNHEX(REPLACE(UUID(), '-', '')), 'SECRETARY_GENERAL', 'Secrétaire général', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'SECRETARY_GENERAL');

INSERT INTO roles (id, name, description, created_at, updated_at)
SELECT UNHEX(REPLACE(UUID(), '-', '')), 'DIRECTOR', 'Directeur', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'DIRECTOR');

INSERT INTO roles (id, name, description, created_at, updated_at)
SELECT UNHEX(REPLACE(UUID(), '-', '')), 'SERVICE_HEAD', 'Chef de service', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'SERVICE_HEAD');

INSERT INTO roles (id, name, description, created_at, updated_at)
SELECT UNHEX(REPLACE(UUID(), '-', '')), 'AGENT', 'Agent traitant', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'AGENT');

INSERT INTO roles (id, name, description, created_at, updated_at)
SELECT UNHEX(REPLACE(UUID(), '-', '')), 'SUPPORT', 'Support / Courrier', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'SUPPORT');

INSERT INTO roles (id, name, description, created_at, updated_at)
SELECT UNHEX(REPLACE(UUID(), '-', '')), 'READER', 'Lecteur', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'READER');

INSERT INTO roles (id, name, description, created_at, updated_at)
SELECT UNHEX(REPLACE(UUID(), '-', '')), 'REGIONAL_DIRECTOR', 'Directeur régional (DRTP)', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'REGIONAL_DIRECTOR');

SELECT name FROM roles ORDER BY name;
