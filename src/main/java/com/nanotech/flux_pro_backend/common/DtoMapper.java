package com.nanotech.flux_pro_backend.common;

import com.nanotech.flux_pro_backend.auth.dto.OrganisationSummaryDto;
import com.nanotech.flux_pro_backend.auth.dto.UserProfileDto;
import com.nanotech.flux_pro_backend.organisation.Organisation;
import com.nanotech.flux_pro_backend.organisation.dto.OrganisationTreeDto;
import com.nanotech.flux_pro_backend.utilisateur.Utilisateur;
import com.nanotech.flux_pro_backend.utilisateur.dto.UtilisateurResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static OrganisationSummaryDto toSummary(Organisation org) {
        return new OrganisationSummaryDto(org.getId(), org.getCode(), org.getNom());
    }

    public static UserProfileDto toProfile(Utilisateur u) {
        return new UserProfileDto(
                u.getId(),
                u.getEmail(),
                u.getNom(),
                u.getPrenom(),
                u.getRole(),
                toSummary(u.getOrganisation()),
                u.isMustChangePassword());
    }

    public static UtilisateurResponse toResponse(Utilisateur u) {
        return new UtilisateurResponse(
                u.getId(),
                u.getMatricule(),
                u.getEmail(),
                u.getNom(),
                u.getPrenom(),
                u.getTelephone(),
                u.getRole(),
                toSummary(u.getOrganisation()),
                u.getFonction(),
                u.isActif(),
                u.isMustChangePassword());
    }

    public static List<OrganisationTreeDto> buildTree(List<Organisation> all) {
        Map<UUID, List<Organisation>> byParent = all.stream()
                .filter(o -> o.getParent() != null)
                .collect(Collectors.groupingBy(o -> o.getParent().getId()));

        return all.stream()
                .filter(o -> o.getParent() == null)
                .sorted(Comparator.comparing(Organisation::getCode))
                .map(root -> toTreeNode(root, byParent))
                .toList();
    }

    private static OrganisationTreeDto toTreeNode(Organisation org, Map<UUID, List<Organisation>> byParent) {
        List<OrganisationTreeDto> children = byParent.getOrDefault(org.getId(), List.of()).stream()
                .sorted(Comparator.comparing(Organisation::getCode))
                .map(child -> toTreeNode(child, byParent))
                .toList();
        return new OrganisationTreeDto(
                org.getId(), org.getCode(), org.getNom(), org.getType(), org.isActif(), children);
    }
}
