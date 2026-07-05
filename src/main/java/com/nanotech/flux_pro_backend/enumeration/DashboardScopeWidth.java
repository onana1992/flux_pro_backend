package com.nanotech.flux_pro_backend.enumeration;

/**
 * Largeur du périmètre organisationnel résolu pour l'appelant du module DSH (cf. SPEC-DSH.md §4.2).
 * Purement informatif : sert au frontend à décider quels widgets afficher, sans jamais
 * s'appuyer sur le nom du rôle de l'utilisateur.
 */
public enum DashboardScopeWidth {
    /** Aucun subordonné dans le périmètre : seule l'activité personnelle a du sens. */
    SELF,
    /** Le périmètre contient plusieurs organisations descendantes (chef de service, directeur...). */
    SUBTREE,
    /** Périmètre régional (toutes les organisations d'une même racine régionale). */
    REGIONAL,
    /** Périmètre national/transversal (SUPER_ADMIN, BUSINESS_ADMIN, SECRETARY_GENERAL, EXECUTIVE_OFFICE). */
    GLOBAL
}
