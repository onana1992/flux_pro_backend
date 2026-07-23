package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public record PassageTransmitRequest(
        String comment,
        UUID nextResponsibleUserId,
        @Valid List<PassageNextAssignmentRequest> nextAssignments,
        /** Copies informées pour le(s) maillon(s) de l'étape suivante (CHN-09). */
        List<UUID> nextCcUserIds
) {
}
