package com.nanotech.flux_pro_backend.config;

import com.nanotech.flux_pro_backend.service.AlertDigestRecipientRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
@RequiredArgsConstructor
public class AlertDigestRecipientRoleDataInitializer implements ApplicationRunner {

    private final AlertDigestRecipientRoleService service;

    @Override
    public void run(ApplicationArguments args) {
        service.seedDefaultsIfEmpty();
    }
}
