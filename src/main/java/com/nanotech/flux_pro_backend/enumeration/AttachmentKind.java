package com.nanotech.flux_pro_backend.enumeration;

/**
 * Niveau métier d'une pièce jointe :
 * <ul>
 *   <li>{@code CREATION} — dépôt à la création / brouillon (interne ou portail)</li>
 *   <li>{@code PASSAGE} — ajouté par le responsable d'un maillon</li>
 *   <li>{@code CLOSURE} — dépôt à la clôture ; peut être rendu visible sur le portail</li>
 * </ul>
 */
public enum AttachmentKind {
    CREATION,
    PASSAGE,
    CLOSURE
}
