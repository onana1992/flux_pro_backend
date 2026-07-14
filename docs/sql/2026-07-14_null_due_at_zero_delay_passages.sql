-- Objectif : supprimer l'échéance artificielle des maillons à délai 0 (ex. clôture).
-- Contexte : calculateDueDate(0) renvoyait receivedAt → overdue immédiat.
-- Tables impactées : file_passages (jointure chain_step_templates)
-- Prérequis : aucun ; correctif de données uniquement (pas de DDL).

UPDATE file_passages fp
INNER JOIN chain_step_templates cst ON cst.id = fp.chain_step_template_id
SET fp.due_at = NULL
WHERE cst.delay_value <= 0
  AND fp.due_at IS NOT NULL;

-- rollback (indicatif) : impossible de restaurer l'ancienne due_at sans backup.
-- Si besoin : réaffecter due_at = received_at pour ces passages uniquement.
-- UPDATE file_passages fp
-- INNER JOIN chain_step_templates cst ON cst.id = fp.chain_step_template_id
-- SET fp.due_at = fp.received_at
-- WHERE cst.delay_value <= 0
--   AND fp.received_at IS NOT NULL
--   AND fp.due_at IS NULL;
