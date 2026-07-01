package com.nanotech.flux_pro_backend.utilisateur;

import com.nanotech.flux_pro_backend.common.BaseEntity;
import com.nanotech.flux_pro_backend.organisation.Organisation;
import com.nanotech.flux_pro_backend.security.UserRole;
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

import java.time.Instant;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
public class Utilisateur extends BaseEntity {

    @Column(nullable = false, unique = true, length = 32)
    private String matricule;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(length = 20)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @Column(length = 255)
    private String fonction;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean mustChangePassword = true;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    private Instant lockedUntil;

    @Column(nullable = false)
    private boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suppleant_id")
    private Utilisateur suppleant;
}
