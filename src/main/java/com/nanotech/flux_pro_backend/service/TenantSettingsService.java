package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.dto.request.TenantSettingsRequest;
import com.nanotech.flux_pro_backend.dto.response.TenantConfigResponse;
import com.nanotech.flux_pro_backend.entity.TenantSettings;
import com.nanotech.flux_pro_backend.repository.TenantSettingsRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Configuration déploiement (tenant) — source unique pour fuseau, pays, branding, from email.
 * Cache en mémoire invalidé à chaque mise à jour.
 */
@Service
@Slf4j
public class TenantSettingsService {

    private final TenantSettingsRepository repository;
    private final AtomicReference<TenantSettings> cache = new AtomicReference<>();

    @Value("${fluxpro.tenant.name:MINTP Cameroun}")
    private String defaultTenantName;

    @Value("${fluxpro.tenant.product-name:FluxPro}")
    private String defaultProductName;

    @Value("${fluxpro.tenant.timezone:Africa/Douala}")
    private String defaultTimezone;

    @Value("${fluxpro.tenant.country-code:CM}")
    private String defaultCountryCode;

    @Value("${fluxpro.tenant.reference-prefix:MINTP}")
    private String defaultReferencePrefix;

    @Value("${fluxpro.tenant.badge:Déploiement pilote · MINTP Cameroun}")
    private String defaultBadge;

    @Value("${fluxpro.alerts.from-address:alertes@mintp.cm}")
    private String defaultFromAddress;

    @Value("${fluxpro.alerts.email-redirect-to:}")
    private String defaultEmailRedirectTo;

    public TenantSettingsService(TenantSettingsRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void warmFallback() {
        // Cache mémoire tant que la table n'est pas encore seedée (ApplicationRunner).
        if (cache.get() == null) {
            cache.set(fallbackInMemory());
        }
    }

    public TenantSettings current() {
        TenantSettings cached = cache.get();
        if (cached != null) {
            return cached;
        }
        return reload();
    }

    public ZoneId zoneId() {
        try {
            return ZoneId.of(current().getTimezone());
        } catch (DateTimeException e) {
            return ZoneId.of(defaultTimezone);
        }
    }

    public String countryCode() {
        String code = current().getCountryCode();
        return code == null || code.isBlank() ? defaultCountryCode : code.trim().toUpperCase();
    }

    public String productName() {
        return current().getProductName();
    }

    public String referencePrefix() {
        return current().getReferencePrefix();
    }

    public String fromAddress() {
        return current().getFromAddress();
    }

    public String emailRedirectTo() {
        String redirect = current().getEmailRedirectTo();
        return redirect == null ? "" : redirect.trim();
    }

    public TenantConfigResponse toResponse() {
        TenantSettings s = current();
        return new TenantConfigResponse(
                s.getTenantName(),
                s.getProductName(),
                s.getTimezone(),
                s.getCountryCode(),
                s.getReferencePrefix(),
                s.getBadge(),
                s.getFromAddress(),
                blankToNull(s.getEmailRedirectTo()));
    }

    @Transactional
    public TenantConfigResponse update(TenantSettingsRequest request) {
        validateTimezone(request.timezone());
        TenantSettings settings = repository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(this::newFromDefaults);
        settings.setTenantName(request.tenantName().trim());
        settings.setProductName(request.productName().trim());
        settings.setTimezone(request.timezone().trim());
        settings.setCountryCode(request.countryCode().trim().toUpperCase());
        settings.setReferencePrefix(request.referencePrefix().trim().toUpperCase());
        settings.setBadge(request.badge().trim());
        settings.setFromAddress(request.fromAddress().trim());
        String redirect = request.emailRedirectTo();
        settings.setEmailRedirectTo(redirect == null || redirect.isBlank() ? null : redirect.trim());
        repository.save(settings);
        reload();
        return toResponse();
    }

    @Transactional
    public void ensureSeeded() {
        if (repository.count() > 0) {
            reload();
            return;
        }
        repository.save(newFromDefaults());
        reload();
    }

    private TenantSettings reload() {
        TenantSettings settings = repository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(this::fallbackInMemory);
        cache.set(settings);
        return settings;
    }

    private TenantSettings newFromDefaults() {
        TenantSettings s = new TenantSettings();
        applyDefaults(s);
        return s;
    }

    private TenantSettings fallbackInMemory() {
        TenantSettings s = new TenantSettings();
        applyDefaults(s);
        return s;
    }

    private void applyDefaults(TenantSettings s) {
        s.setTenantName(defaultTenantName);
        s.setProductName(defaultProductName);
        s.setTimezone(defaultTimezone);
        s.setCountryCode(defaultCountryCode.trim().toUpperCase());
        s.setReferencePrefix(defaultReferencePrefix.trim().toUpperCase());
        s.setBadge(defaultBadge);
        s.setFromAddress(defaultFromAddress);
        s.setEmailRedirectTo(
                defaultEmailRedirectTo == null || defaultEmailRedirectTo.isBlank()
                        ? null
                        : defaultEmailRedirectTo.trim());
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone.trim());
        } catch (DateTimeException e) {
            throw AppException.badRequest(
                    "TENANT_TIMEZONE_INVALID", "Invalid timezone: " + timezone, timezone);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
