package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.AlertRuleRequest;
import com.nanotech.flux_pro_backend.dto.response.AlertRuleResponse;
import com.nanotech.flux_pro_backend.mapper.AlertRuleMapper;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.service.AlertRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Règles d'alerte d'un template précis (ALR-06). Aucune route globale : chaque template
 * porte sa propre configuration, appliquée éventuellement depuis le profil de seed CDC §10.2.
 */
@RestController
@RequestMapping("/api/admin/chain-templates/{chainTemplateId}/alert-rules")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @GetMapping
    @RequiresPermission(RbacPermissions.ALERT_RULES_READ)
    public List<AlertRuleResponse> list(@PathVariable UUID chainTemplateId) {
        return alertRuleService.listByTemplate(chainTemplateId).stream().map(AlertRuleMapper::toResponse).toList();
    }

    @GetMapping("/{ruleId}")
    @RequiresPermission(RbacPermissions.ALERT_RULES_READ)
    public AlertRuleResponse getById(@PathVariable UUID chainTemplateId, @PathVariable UUID ruleId) {
        return AlertRuleMapper.toResponse(alertRuleService.getById(chainTemplateId, ruleId));
    }

    @PostMapping
    @RequiresPermission(RbacPermissions.ALERT_RULES_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public AlertRuleResponse create(
            @PathVariable UUID chainTemplateId, @Valid @RequestBody AlertRuleRequest request) {
        return AlertRuleMapper.toResponse(alertRuleService.create(chainTemplateId, request));
    }

    @PutMapping("/{ruleId}")
    @RequiresPermission(RbacPermissions.ALERT_RULES_UPDATE)
    public AlertRuleResponse update(
            @PathVariable UUID chainTemplateId,
            @PathVariable UUID ruleId,
            @Valid @RequestBody AlertRuleRequest request) {
        return AlertRuleMapper.toResponse(alertRuleService.update(chainTemplateId, ruleId, request));
    }

    @PatchMapping("/{ruleId}/activate")
    @RequiresPermission(RbacPermissions.ALERT_RULES_UPDATE)
    public AlertRuleResponse activate(@PathVariable UUID chainTemplateId, @PathVariable UUID ruleId) {
        return AlertRuleMapper.toResponse(alertRuleService.activate(chainTemplateId, ruleId));
    }

    @PatchMapping("/{ruleId}/deactivate")
    @RequiresPermission(RbacPermissions.ALERT_RULES_UPDATE)
    public AlertRuleResponse deactivate(@PathVariable UUID chainTemplateId, @PathVariable UUID ruleId) {
        return AlertRuleMapper.toResponse(alertRuleService.deactivate(chainTemplateId, ruleId));
    }

    @DeleteMapping("/{ruleId}")
    @RequiresPermission(RbacPermissions.ALERT_RULES_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID chainTemplateId, @PathVariable UUID ruleId) {
        alertRuleService.delete(chainTemplateId, ruleId);
    }

    /** Copie le profil de seed CDC §10.2 sur ce template (ALR-F18) — jamais appliqué implicitement. */
    @PostMapping("/apply-default-profile")
    @RequiresPermission(RbacPermissions.ALERT_RULES_CREATE)
    public List<AlertRuleResponse> applyDefaultProfile(
            @PathVariable UUID chainTemplateId,
            @RequestParam(defaultValue = "false") boolean overwriteExisting) {
        return alertRuleService.applyDefaultProfile(chainTemplateId, overwriteExisting).stream()
                .map(AlertRuleMapper::toResponse)
                .toList();
    }
}
