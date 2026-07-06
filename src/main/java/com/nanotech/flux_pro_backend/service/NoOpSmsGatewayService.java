package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.entity.Alert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NoOpSmsGatewayService implements SmsGatewayService {

    @Override
    public void send(Alert alert) {
        log.info("ALR: passerelle SMS non branchée (ALR-09, phase 2) — alerte {} non envoyée par SMS", alert.getId());
        throw AppException.notImplemented(
                "SMS_GATEWAY_NOT_IMPLEMENTED", "SMS gateway not implemented yet (ALR-09, phase 2)");
    }
}
