-- Objectif : restaurer le compte démo SUPER_ADMIN (mot de passe Mintp@2025)
-- Tables impactées : users
-- Prérequis : exécuter manuellement sur MySQL fluxpro

UPDATE users
SET password_hash = '$2a$12$5y/ddaQzbSQZbKx2jRZ8PeB/hpqOD4EiktSUeTYO69TkmzTUo9Qlm',
    must_change_password = 0,
    failed_login_attempts = 0,
    locked_until = NULL
WHERE email = 'e.fotso@mintp.cm';
