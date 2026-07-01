package com.nanotech.flux_pro_backend.auth;

import com.nanotech.flux_pro_backend.common.BaseEntity;
import com.nanotech.flux_pro_backend.utilisateur.Utilisateur;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "login_audit")
@Getter
@Setter
public class LoginAudit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean succes;

    @Column(length = 45)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    @Column(length = 50)
    private String motifEchec;
}
