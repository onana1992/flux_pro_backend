package com.nanotech.flux_pro_backend.dto.response;

import java.util.UUID;

/** Classement des organisations par taux de respect des délais (DSH-07), regroupé par un
 * niveau d'{@code OrganizationType} paramétrable — jamais un niveau organisationnel figé. */
public record OrganizationRankingResponse(
        UUID organizationId,
        String organizationCode,
        String organizationName,
        long closedCount,
        long compliantCount,
        double complianceRate) {
}
