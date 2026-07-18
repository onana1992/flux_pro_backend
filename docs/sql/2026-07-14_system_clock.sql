-- Objectif : horloge artificielle pour le mode test (avance/recul manuel, tick réel).
-- Tables : system_clock
-- Prérequis : aucun ; ddl-auto=none — exécuter manuellement avant démarrage en mode test.

CREATE TABLE IF NOT EXISTS system_clock (
    id                BINARY(16)   NOT NULL PRIMARY KEY,
    -- Instant artificiel au moment de la dernière synchronisation
    artificial_now    DATETIME(6)  NOT NULL,
    -- Instant mur réel correspondant à artificial_now (permet le tick)
    wall_synced_at    DATETIME(6)  NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL
);

-- rollback
-- DROP TABLE IF EXISTS system_clock;
