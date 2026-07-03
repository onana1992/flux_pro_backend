package com.nanotech.flux_pro_backend.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Vérifie si un template est utilisé par des dossiers en cours (Sprint 2 / CHN-PASS).
 * Retourne toujours false tant que le module dossiers n'est pas implémenté.
 */
@Service
public class ChainTemplateUsageService {

    public boolean hasInProgressFiles(UUID templateId) {
        return false;
    }
}
