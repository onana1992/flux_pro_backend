package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AlertException;
import com.nanotech.flux_pro_backend.dto.request.AlertTypeRequest;
import com.nanotech.flux_pro_backend.entity.AlertType;
import com.nanotech.flux_pro_backend.repository.AlertRepository;
import com.nanotech.flux_pro_backend.repository.AlertRuleRepository;
import com.nanotech.flux_pro_backend.repository.AlertTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD du catalogue des types d'alerte (ALR-F17). Un type n'est jamais figé dans une
 * énumération Java : créer, modifier ou désactiver un type est une simple opération de
 * données, sans redéploiement (cf. docs/SPEC-ALR.md §6.2).
 */
@Service
@RequiredArgsConstructor
public class AlertTypeService {

    private final AlertTypeRepository alertTypeRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;

    @Transactional(readOnly = true)
    public List<AlertType> listActive() {
        return alertTypeRepository.findByActiveTrueOrderByLabelAsc();
    }

    @Transactional(readOnly = true)
    public List<AlertType> listAll() {
        return alertTypeRepository.findAllByOrderByLabelAsc();
    }

    @Transactional(readOnly = true)
    public AlertType getById(UUID id) {
        return alertTypeRepository.findById(id)
                .orElseThrow(() -> AlertException.notFound("ALERT_TYPE_NOT_FOUND", "Alert type not found"));
    }

    @Transactional(readOnly = true)
    public AlertType getByCode(String code) {
        return alertTypeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> AlertException.notFound(
                        "ALERT_TYPE_NOT_FOUND_BY_CODE", "Alert type not found: " + code, code));
    }

    @Transactional
    public AlertType create(AlertTypeRequest request) {
        if (alertTypeRepository.existsByCodeIgnoreCase(request.code())) {
            throw AlertException.badRequest(
                    "ALERT_TYPE_CODE_EXISTS", "Alert type code already in use: " + request.code(), request.code());
        }
        AlertType type = new AlertType();
        type.setCode(request.code().trim().toUpperCase());
        type.setSystemDefined(false);
        applyRequest(type, request);
        return alertTypeRepository.save(type);
    }

    @Transactional
    public AlertType update(UUID id, AlertTypeRequest request) {
        AlertType type = getById(id);
        if (!type.getCode().equalsIgnoreCase(request.code())) {
            throw AlertException.badRequest("ALERT_TYPE_CODE_IMMUTABLE", "Alert type code cannot be changed");
        }
        applyRequest(type, request);
        return alertTypeRepository.save(type);
    }

    @Transactional
    public AlertType activate(UUID id) {
        AlertType type = getById(id);
        type.setActive(true);
        return alertTypeRepository.save(type);
    }

    @Transactional
    public AlertType deactivate(UUID id) {
        AlertType type = getById(id);
        type.setActive(false);
        return alertTypeRepository.save(type);
    }

    @Transactional
    public void delete(UUID id) {
        AlertType type = getById(id);
        if (type.isSystemDefined()) {
            throw AlertException.conflict(
                    "ALERT_TYPE_SYSTEM_DEFINED", "Cannot delete a system-defined alert type");
        }
        if (alertRuleRepository.existsByAlertTypeId(id) || alertRepository.existsByAlertTypeId(id)) {
            throw AlertException.conflict(
                    "ALERT_TYPE_IN_USE", "Cannot delete alert type: still referenced by alert rule(s)");
        }
        alertTypeRepository.delete(type);
    }

    private void applyRequest(AlertType type, AlertTypeRequest request) {
        type.setLabel(request.label().trim());
        type.setDescription(request.description());
        type.setEmailTemplateCode(
                request.emailTemplateCode() != null && !request.emailTemplateCode().isBlank()
                        ? request.emailTemplateCode().trim()
                        : null);
        type.setActive(request.active());
    }
}
