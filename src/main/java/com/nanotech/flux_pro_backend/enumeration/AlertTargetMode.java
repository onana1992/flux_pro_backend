package com.nanotech.flux_pro_backend.enumeration;

/**
 * Mode de résolution du destinataire d'une {@link com.nanotech.flux_pro_backend.entity.AlertRule}.
 * Aucun rôle n'est codé en dur dans le moteur : ROLE délègue entièrement à AlertRule.targetRole
 * (n'importe quelle valeur de UserRole), configurable par l'admin métier (ALR-06).
 */
public enum AlertTargetMode {
    CURRENT_RESPONSIBLE,
    ROLE
}
