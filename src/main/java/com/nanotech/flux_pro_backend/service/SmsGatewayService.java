package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.Alert;

/**
 * Passerelle SMS opérateur local (ALR-09 — phase 2, priorité Could).
 * L'implémentation par défaut ({@link NoOpSmsGatewayService}) est un stub explicite ;
 * le canal SMS reste désactivé par défaut (fluxpro.alerts.sms.enabled=false).
 */
public interface SmsGatewayService {

    void send(Alert alert);
}
