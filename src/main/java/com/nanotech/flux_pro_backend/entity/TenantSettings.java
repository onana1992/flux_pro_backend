package com.nanotech.flux_pro_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration unique du déploiement (tenant). Une seule ligne en base ;
 * seedée depuis {@code fluxpro.tenant.*} / {@code fluxpro.alerts.*} si absente.
 */
@Entity
@Table(name = "tenant_settings")
@Getter
@Setter
public class TenantSettings extends BaseEntity {

    @Column(name = "tenant_name", nullable = false, length = 150)
    private String tenantName;

    @Column(name = "product_name", nullable = false, length = 80)
    private String productName;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "reference_prefix", nullable = false, length = 20)
    private String referencePrefix;

    @Column(nullable = false, length = 200)
    private String badge;

    @Column(name = "from_address", nullable = false, length = 200)
    private String fromAddress;

    @Column(name = "email_redirect_to", length = 200)
    private String emailRedirectTo;
}
