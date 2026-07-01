-- Objectif : corriger les tables RBAC créées avec CHAR(36) alors que users.id est BINARY(16)
-- Tables impactées : roles, permissions, user_roles, role_permissions
-- Prérequis : exécuter uniquement si SHOW CREATE TABLE roles montre `id` char(36)
-- ATTENTION : supprime les données RBAC existantes (tables vides ou à re-seeder au redémarrage)

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS roles;

SET FOREIGN_KEY_CHECKS = 1;

-- Puis exécuter : 2026-07-02_rbac_roles_permissions.sql
