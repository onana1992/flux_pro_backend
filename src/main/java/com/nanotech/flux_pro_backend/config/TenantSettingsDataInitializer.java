package com.nanotech.flux_pro_backend.config;

import com.nanotech.flux_pro_backend.service.TenantSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class TenantSettingsDataInitializer implements ApplicationRunner {

    private final TenantSettingsService tenantSettingsService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            tenantSettingsService.ensureSeeded();
            var current = tenantSettingsService.current();
            log.info(
                    "Tenant settings prêts — name={}, tz={}, country={}, prefix={}",
                    current.getTenantName(),
                    current.getTimezone(),
                    current.getCountryCode(),
                    current.getReferencePrefix());
        } catch (Exception e) {
            log.error(
                    "Tenant settings : table inaccessible — exécutez docs/sql/2026-07-22_tenant_settings.sql — {}",
                    e.getMessage());
        }
    }
}
