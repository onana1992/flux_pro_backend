package com.nanotech.flux_pro_backend.mapper;

import com.nanotech.flux_pro_backend.dto.response.OrganizationDetailResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationSummaryResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationTreeResponse;
import com.nanotech.flux_pro_backend.dto.response.OrganizationTypeResponse;
import com.nanotech.flux_pro_backend.dto.response.PermissionResponse;
import com.nanotech.flux_pro_backend.dto.response.RoleResponse;
import com.nanotech.flux_pro_backend.dto.response.RoleSummaryResponse;
import com.nanotech.flux_pro_backend.dto.response.UserProfileResponse;
import com.nanotech.flux_pro_backend.dto.response.UserResponse;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.OrganizationType;
import com.nanotech.flux_pro_backend.entity.Permission;
import com.nanotech.flux_pro_backend.entity.Role;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.security.RbacAuthorityService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static OrganizationTypeResponse toTypeResponse(OrganizationType type) {
        return new OrganizationTypeResponse(
                type.getId(),
                type.getCode(),
                type.getName(),
                type.getNameEn(),
                type.getDescription(),
                type.getColor(),
                type.getSortOrder(),
                type.isAllowsRoot(),
                type.isRegionalScope(),
                type.isActive());
    }

    public static OrganizationSummaryResponse toSummary(Organization org) {
        return new OrganizationSummaryResponse(org.getId(), org.getCode(), org.getName());
    }

    public static OrganizationDetailResponse toDetail(Organization org) {
        Organization parent = org.getParent();
        OrganizationType type = org.getOrganizationType();
        return new OrganizationDetailResponse(
                org.getId(),
                org.getCode(),
                org.getName(),
                type.getId(),
                type.getCode(),
                type.getName(),
                parent != null ? parent.getId() : null,
                parent != null ? parent.getCode() : null,
                org.isActive());
    }

    public static UserProfileResponse toProfile(User user, RbacAuthorityService.RbacAuthorities authorities) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getLastName(),
                user.getFirstName(),
                user.getRole(),
                toSummary(user.getOrganization()),
                user.isMustChangePassword(),
                authorities.roleNames(),
                authorities.permissionNames());
    }

    public static UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getLastName(),
                user.getFirstName(),
                user.getRole(),
                toSummary(user.getOrganization()),
                user.isMustChangePassword(),
                List.of(user.getRole().name()),
                List.of());
    }

    public static UserResponse toResponse(User user) {
        List<RoleSummaryResponse> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                        .map(r -> new RoleSummaryResponse(r.getId(), r.getName()))
                        .toList();
        User substitute = user.getSubstitute();
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
                user.isOrganizationHead(),
                substitute != null ? substitute.getId() : null,
                substitute != null
                        ? substitute.getLastName() + " " + substitute.getFirstName()
                        : null,
                user.isMustChangePassword(),
                roles);
    }

    public static PermissionResponse toPermissionResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getName(),
                permission.getResource(),
                permission.getAction(),
                permission.getDescription(),
                permission.getCreatedAt());
    }

    public static RoleResponse toRoleResponse(Role role) {
        List<PermissionResponse> permissions = role.getPermissions() == null
                ? List.of()
                : role.getPermissions().stream().map(DtoMapper::toPermissionResponse).toList();
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isSystemRole(),
                role.getCreatedAt(),
                permissions);
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
                org.getId(),
                org.getCode(),
                org.getName(),
                toTypeResponse(org.getOrganizationType()),
                org.isActive(),
                children);
    }
}
