package com.nanotech.flux_pro_backend.organisation.dto;

import com.nanotech.flux_pro_backend.organisation.OrganisationType;

import java.util.List;
import java.util.UUID;

public record OrganisationTreeDto(
        UUID id,
        String code,
        String nom,
        OrganisationType type,
        boolean actif,
        List<OrganisationTreeDto> children
) {
}
