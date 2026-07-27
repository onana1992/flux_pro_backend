package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.dto.request.PreconfiguredDossierRequest;
import com.nanotech.flux_pro_backend.dto.request.PreconfiguredStepAssignmentRequest;
import com.nanotech.flux_pro_backend.entity.ChainStepTemplate;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.entity.PreconfiguredDossier;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.repository.ChainTemplateRepository;
import com.nanotech.flux_pro_backend.repository.FileTypeRepository;
import com.nanotech.flux_pro_backend.repository.PreconfiguredDossierRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import com.nanotech.flux_pro_backend.service.portal.FormSchemaValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreconfiguredDossierService {

    private final PreconfiguredDossierRepository preconfiguredDossierRepository;
    private final FileTypeRepository fileTypeRepository;
    private final ChainTemplateRepository chainTemplateRepository;
    private final UserRepository userRepository;
    private final FormSchemaValidator formSchemaValidator;

    @Transactional(readOnly = true)
    public List<PreconfiguredDossier> listAll() {
        return preconfiguredDossierRepository.findAllWithChainOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public PreconfiguredDossier getById(UUID id) {
        return preconfiguredDossierRepository.findByIdWithChain(id)
                .orElseThrow(() -> AppException.notFound(
                        "PRECONFIGURED_DOSSIER_NOT_FOUND", "Preconfigured dossier not found"));
    }

    @Transactional(readOnly = true)
    public PreconfiguredDossier getByCode(String code) {
        return preconfiguredDossierRepository.findByCodeIgnoreCaseWithChain(code)
                .orElseThrow(() -> AppException.notFound(
                        "PRECONFIGURED_DOSSIER_NOT_FOUND",
                        "Preconfigured dossier not found: " + code,
                        code));
    }

    @Transactional
    public PreconfiguredDossier create(PreconfiguredDossierRequest request) {
        if (preconfiguredDossierRepository.existsByCodeIgnoreCase(request.code())) {
            throw AppException.badRequest(
                    "PRECONFIGURED_DOSSIER_CODE_IN_USE",
                    "Preconfigured dossier code already in use: " + request.code(),
                    request.code());
        }
        PreconfiguredDossier dossier = new PreconfiguredDossier();
        applyRequest(dossier, request, true);
        return preconfiguredDossierRepository.save(dossier);
    }

    @Transactional
    public PreconfiguredDossier update(UUID id, PreconfiguredDossierRequest request) {
        PreconfiguredDossier dossier = getById(id);
        if (!dossier.getCode().equalsIgnoreCase(request.code())) {
            throw AppException.badRequest(
                    "PRECONFIGURED_DOSSIER_CODE_IMMUTABLE",
                    "Preconfigured dossier code cannot be changed");
        }
        applyRequest(dossier, request, false);
        return preconfiguredDossierRepository.save(dossier);
    }

    @Transactional
    public PreconfiguredDossier deactivate(UUID id) {
        PreconfiguredDossier dossier = getById(id);
        dossier.setActive(false);
        dossier.setPortalEnabled(false);
        return preconfiguredDossierRepository.save(dossier);
    }

    @Transactional
    public void delete(UUID id) {
        PreconfiguredDossier dossier = getById(id);
        preconfiguredDossierRepository.delete(dossier);
    }

    /** Parse stepAssignments JSON → map stepId → userId. */
    public static Map<UUID, UUID> parseStepAssignments(Map<String, Object> raw) {
        Map<UUID, UUID> result = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                continue;
            }
            try {
                result.put(UUID.fromString(e.getKey()), UUID.fromString(String.valueOf(e.getValue())));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entries
            }
        }
        return result;
    }

    private void applyRequest(PreconfiguredDossier dossier, PreconfiguredDossierRequest request, boolean isCreate) {
        if (isCreate) {
            dossier.setCode(request.code().trim().toUpperCase());
        }

        String fileTypeCode = request.fileTypeCode().trim().toUpperCase();
        if (fileTypeRepository.findByCodeIgnoreCase(fileTypeCode).isEmpty()) {
            throw AppException.badRequest(
                    "FILE_TYPE_NOT_FOUND_BY_CODE",
                    "File type not found: " + fileTypeCode,
                    fileTypeCode);
        }
        dossier.setFileTypeCode(fileTypeCode);
        dossier.setName(request.name().trim());
        dossier.setNameEn(blankToNull(request.nameEn()));
        dossier.setDescription(request.description());
        dossier.setDirectionCode(
                request.directionCode() != null && !request.directionCode().isBlank()
                        ? request.directionCode().trim().toUpperCase()
                        : null);
        dossier.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        dossier.setActive(Boolean.TRUE.equals(request.active()));

        boolean portalEnabled = Boolean.TRUE.equals(request.portalEnabled());
        dossier.setPortalEnabled(portalEnabled);

        ChainTemplate chain = null;
        if (request.chainTemplateId() != null) {
            chain = chainTemplateRepository.findByIdWithSteps(request.chainTemplateId())
                    .orElseThrow(() -> AppException.notFound(
                            "CHAIN_TEMPLATE_NOT_FOUND", "Chain template not found"));
            dossier.setChainTemplate(chain);
        } else {
            dossier.setChainTemplate(null);
        }

        if (request.stepAssignments() != null) {
            dossier.setStepAssignments(toAssignmentMap(request.stepAssignments(), chain));
        } else if (chain == null) {
            dossier.setStepAssignments(new HashMap<>());
        }

        if (request.formSchema() != null) {
            assertSchemaStructure(request.formSchema());
            dossier.setFormSchema(request.formSchema());
        }
        if (request.requiredAttachmentKeys() != null) {
            dossier.setRequiredAttachmentKeys(new ArrayList<>(request.requiredAttachmentKeys()));
        }

        if (portalEnabled) {
            if (request.portalAudience() == null && dossier.getPortalAudience() == null) {
                throw AppException.badRequest(
                        "PORTAL_AUDIENCE_REQUIRED",
                        "portalAudience is required when portal is enabled");
            }
            if (request.portalAudience() != null) {
                dossier.setPortalAudience(request.portalAudience());
            }
            assertPortalReady(dossier);
        } else if (request.portalAudience() != null) {
            dossier.setPortalAudience(request.portalAudience());
        }
    }

    private Map<String, Object> toAssignmentMap(
            List<PreconfiguredStepAssignmentRequest> assignments, ChainTemplate chain) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (assignments == null || assignments.isEmpty()) {
            return map;
        }
        Set<UUID> knownSteps = chain == null || chain.getSteps() == null
                ? Set.of()
                : chain.getSteps().stream().map(ChainStepTemplate::getId).collect(Collectors.toSet());
        Map<UUID, ChainStepTemplate> stepById = chain == null || chain.getSteps() == null
                ? Map.of()
                : chain.getSteps().stream().collect(Collectors.toMap(ChainStepTemplate::getId, s -> s));

        for (PreconfiguredStepAssignmentRequest a : assignments) {
            if (a.chainStepTemplateId() == null || a.responsibleUserId() == null) {
                continue;
            }
            if (!knownSteps.isEmpty() && !knownSteps.contains(a.chainStepTemplateId())) {
                throw AppException.badRequest(
                        "PASSAGE_ASSIGNMENT_UNKNOWN_STEP",
                        "Assignment refers to a step that is not part of the selected chain");
            }
            User user = userRepository.findByIdWithOrganization(a.responsibleUserId())
                    .orElseThrow(() -> AppException.notFound("USER_NOT_FOUND", "User not found"));
            if (!user.isActive()) {
                throw AppException.badRequest(
                        "PORTAL_RESPONSIBLE_INACTIVE",
                        "Responsible user must be active: " + user.getEmail(),
                        user.getEmail());
            }
            ChainStepTemplate step = stepById.get(a.chainStepTemplateId());
            if (step != null && user.getRole() != step.getResponsibleRole()) {
                throw AppException.badRequest(
                        "PORTAL_RESPONSIBLE_ROLE_MISMATCH",
                        "User role must match step role " + step.getResponsibleRole()
                                + " for step: " + step.getLabel(),
                        step.getLabel());
            }
            map.put(a.chainStepTemplateId().toString(), a.responsibleUserId().toString());
        }
        return map;
    }

    private void assertPortalReady(PreconfiguredDossier dossier) {
        if (dossier.getFormSchema() == null || dossier.getFormSchema().isEmpty()) {
            throw AppException.badRequest(
                    "PORTAL_FORM_SCHEMA_MISSING",
                    "formSchema is required when portal is enabled");
        }
        if (dossier.getChainTemplate() == null) {
            throw AppException.badRequest(
                    "PORTAL_CHAIN_REQUIRED",
                    "chainTemplateId is required when portal is enabled");
        }
        if (!dossier.getChainTemplate().isActive()) {
            throw AppException.badRequest(
                    "PORTAL_CHAIN_INACTIVE",
                    "Linked chain template must be active");
        }
        if (dossier.getPortalAudience() == null) {
            throw AppException.badRequest(
                    "PORTAL_AUDIENCE_REQUIRED",
                    "portalAudience is required when portal is enabled");
        }

        ChainTemplate chain = chainTemplateRepository.findByIdWithSteps(dossier.getChainTemplate().getId())
                .orElse(dossier.getChainTemplate());
        List<ChainStepTemplate> steps = chain.getSteps() == null ? List.of() : chain.getSteps();
        if (steps.isEmpty()) {
            throw AppException.badRequest("PASSAGE_CHAIN_EMPTY", "Chain template has no steps");
        }
        Integer firstStage = PassageStageHelper.distinctStagesFromSteps(steps).stream()
                .findFirst()
                .orElseThrow();
        Map<UUID, UUID> assignments = parseStepAssignments(dossier.getStepAssignments());
        for (ChainStepTemplate step : steps) {
            if (step.getStepOrder() == firstStage && !assignments.containsKey(step.getId())) {
                throw AppException.badRequest(
                        "PASSAGE_FIRST_ASSIGNMENT_REQUIRED",
                        "A responsible user must be chosen for the first stage: " + step.getLabel(),
                        step.getLabel());
            }
        }
    }

    private void assertSchemaStructure(Map<String, Object> formSchema) {
        if (formSchema == null || !(formSchema.get("fields") instanceof List<?> fields) || fields.isEmpty()) {
            throw AppException.badRequest("PORTAL_FORM_SCHEMA_INVALID", "Form schema must declare fields");
        }
        Map<String, Object> stubData = new LinkedHashMap<>();
        for (Object fieldObj : fields) {
            if (!(fieldObj instanceof Map<?, ?> fieldMap)) {
                continue;
            }
            Object key = fieldMap.get("key");
            Object type = fieldMap.get("type");
            if (key == null || type == null) {
                continue;
            }
            String t = String.valueOf(type).toUpperCase();
            stubData.put(String.valueOf(key), switch (t) {
                case "NUMBER" -> 0;
                case "BOOLEAN" -> false;
                case "DATE" -> "2026-01-01";
                case "DATETIME" -> "2026-01-01T00:00:00";
                case "MULTI_ENUM" -> {
                    Object options = fieldMap.get("options");
                    if (options instanceof List<?> list && !list.isEmpty()) {
                        yield List.of(String.valueOf(list.get(0)));
                    }
                    yield List.of("X");
                }
                case "ENUM" -> {
                    Object options = fieldMap.get("options");
                    if (options instanceof List<?> list && !list.isEmpty()) {
                        yield String.valueOf(list.get(0));
                    }
                    yield "X";
                }
                default -> "x";
            });
        }
        formSchemaValidator.validate(formSchema, stubData);
    }

    private static String blankToNull(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}
