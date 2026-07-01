package com.nanotech.flux_pro_backend.organisation;

import com.nanotech.flux_pro_backend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "organisation")
@Getter
@Setter
public class Organisation extends BaseEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrganisationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Organisation parent;

    @Column(nullable = false)
    private boolean actif = true;
}
