package com.nanotech.flux_pro_backend.mapper;

import com.nanotech.flux_pro_backend.dto.response.OrganizationSummaryResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationTreeResponse;
import com.nanotech.flux_pro_backend.dto.response.UserProfileResponse;
import com.nanotech.flux_pro_backend.dto.response.UserResponse;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.User;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static OrganizationSummaryResponse toSummary(Organization org) {
        return new OrganizationSummaryResponse(org.getId(), org.getCode(), org.getName());
    }

    public static UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getLastName(),
                user.getFirstName(),
                user.getRole(),
                toSummary(user.getOrganization()),
                user.isMustChangePassword());
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getStaffNumber(),
                user.getEmail(),
                user.getLastName(),
                user.getFirstName(),
                user.getPhone(),
                user.getRole(),
                toSummary(user.getOrganization()),
                user.getJobTitle(),
                user.isActive(),
                user.isMustChangePassword());
    }

    public static List<OrganizationTreeResponse> buildTree(List<Organization> all) {
        Map<UUID, List<Organization>> byParent = all.stream()
                .filter(o -> o.getParent() != null)
                .collect(Collectors.groupingBy(o -> o.getParent().getId()));

        return all.stream()
                .filter(o -> o.getParent() == null)
                .sorted(Comparator.comparing(Organization::getCode))
                .map(root -> toTreeNode(root, byParent))
                .toList();
    }

    private static OrganizationTreeResponse toTreeNode(Organization org, Map<UUID, List<Organization>> byParent) {
        List<OrganizationTreeResponse> children = byParent.getOrDefault(org.getId(), List.of()).stream()
                .sorted(Comparator.comparing(Organization::getCode))
                .map(child -> toTreeNode(child, byParent))
                .toList();
        return new OrganizationTreeResponse(
                org.getId(), org.getCode(), org.getName(), org.getType(), org.isActive(), children);
    }
}
