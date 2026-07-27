package com.nanotech.flux_pro_backend.mapper;

import com.nanotech.flux_pro_backend.dto.response.PreconfiguredDossierResponse;
import com.nanotech.flux_pro_backend.dto.response.PreconfiguredStepAssignmentResponse;
import com.nanotech.flux_pro_backend.entity.ChainStepTemplate;
import com.nanotech.flux_pro_backend.entity.PreconfiguredDossier;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.service.PreconfiguredDossierService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PreconfiguredDossierMapper {

    private PreconfiguredDossierMapper() {
    }

    public static PreconfiguredDossierResponse toResponse(PreconfiguredDossier d) {
        return toResponse(d, Map.of());
    }

    public static PreconfiguredDossierResponse toResponse(PreconfiguredDossier d, Map<UUID, User> usersById) {
        var chain = d.getChainTemplate();
        List<PreconfiguredStepAssignmentResponse> assignments = new ArrayList<>();
        Map<UUID, UUID> parsed = PreconfiguredDossierService.parseStepAssignments(d.getStepAssignments());

        if (chain != null && chain.getSteps() != null) {
            List<ChainStepTemplate> steps = chain.getSteps().stream()
                    .sorted(Comparator.comparingInt(ChainStepTemplate::getStepOrder)
                            .thenComparing(ChainStepTemplate::getLabel, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            for (ChainStepTemplate step : steps) {
                assignments.add(toStepAssignment(step, parsed.get(step.getId()), usersById));
            }
        } else {
            for (Map.Entry<UUID, UUID> e : parsed.entrySet()) {
                assignments.add(toStepAssignment(null, e.getKey(), e.getValue(), usersById));
            }
        }

        return new PreconfiguredDossierResponse(
                d.getId(),
                d.getCode(),
                d.getName(),
                d.getNameEn(),
                d.getDescription(),
                d.getFileTypeCode(),
                chain != null ? chain.getId() : null,
                chain != null ? chain.getCode() : null,
                chain != null ? chain.getName() : null,
                assignments,
                d.getDirectionCode(),
                d.getSortOrder(),
                d.isActive(),
                d.isPortalEnabled(),
                d.getPortalAudience(),
                d.getFormSchema(),
                d.getRequiredAttachmentKeys() != null ? d.getRequiredAttachmentKeys() : List.of());
    }

    private static PreconfiguredStepAssignmentResponse toStepAssignment(
            ChainStepTemplate step, UUID userId, Map<UUID, User> usersById) {
        return toStepAssignment(step, step != null ? step.getId() : null, userId, usersById);
    }

    private static PreconfiguredStepAssignmentResponse toStepAssignment(
            ChainStepTemplate step, UUID stepId, UUID userId, Map<UUID, User> usersById) {
        User user = userId != null ? usersById.get(userId) : null;
        String name = user != null
                ? (user.getFirstName() + " " + user.getLastName()).trim()
                : null;
        return new PreconfiguredStepAssignmentResponse(
                stepId,
                step != null ? step.getLabel() : null,
                step != null ? step.getStepOrder() : 0,
                step != null ? step.getResponsibleRole() : null,
                userId,
                name,
                user != null ? user.getEmail() : null,
                user != null && user.getOrganization() != null ? user.getOrganization().getCode() : null,
                user != null && user.getOrganization() != null ? user.getOrganization().getName() : null);
    }
}
