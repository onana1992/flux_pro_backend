package com.nanotech.flux_pro_backend.entity;

import com.nanotech.flux_pro_backend.converter.JsonMapConverter;
import com.nanotech.flux_pro_backend.converter.JsonStringListConverter;
import com.nanotech.flux_pro_backend.enumeration.PortalAudience;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dossier préconfiguré : bundle catalogue (type + formulaire dédié + circuit + responsables).
 * Le formulaire n'est pas réutilisable — schéma JSON propriétaire de cette entité.
 */
@Entity
@Table(name = "preconfigured_dossiers")
@Getter
@Setter
public class PreconfiguredDossier extends BaseEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_en")
    private String nameEn;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Classification métier (FileType.code) — pas le propriétaire du formulaire. */
    @Column(name = "file_type_code", nullable = false, length = 32)
    private String fileTypeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chain_template_id")
    private ChainTemplate chainTemplate;

    /**
     * Affectations responsables par maillon : clé = chainStepTemplateId, valeur = userId.
     * Comme à l'init circuit d'un dossier : 1er stage obligatoire, suivants optionnels.
     */
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "step_assignments", columnDefinition = "LONGTEXT")
    private Map<String, Object> stepAssignments = new HashMap<>();

    @Column(name = "direction_code", length = 32)
    private String directionCode;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "portal_enabled", nullable = false)
    private boolean portalEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "portal_audience", length = 20)
    private PortalAudience portalAudience;

    /** Formulaire de demande propriétaire (1:1, non réutilisable). */
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "form_schema", columnDefinition = "LONGTEXT")
    private Map<String, Object> formSchema;

    @Convert(converter = JsonStringListConverter.class)
    @Column(name = "required_attachment_keys", columnDefinition = "LONGTEXT")
    private List<String> requiredAttachmentKeys = new ArrayList<>();
}
