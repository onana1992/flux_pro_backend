package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.dto.request.UserRequest;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.FilePassageRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final OrganizationScopeService organizationScopeService;
    private final OrganizationRepository organizationRepository;
    private final FilePassageRepository filePassageRepository;

    public boolean canReadUsers(SecurityUser actor) {
        return organizationScopeService.hasGlobalScope(actor)
                || actor.getRole() == UserRole.BUSINESS_ADMIN
                || actor.getRole() == UserRole.DIRECTOR
                || actor.getRole() == UserRole.SERVICE_HEAD
                || actor.getRole() == UserRole.REGIONAL_DIRECTOR;
    }

    public boolean canWriteUsers(SecurityUser actor) {
        return actor.getRole() == UserRole.SUPER_ADMIN
                || actor.getRole() == UserRole.BUSINESS_ADMIN;
    }

    @Transactional(readOnly = true)
    public OrganizationScopeService.ScopeFilter resolveUserSearchScope(SecurityUser actor) {
        return organizationScopeService.resolveScopeFilter(actor);
    }

    @Transactional(readOnly = true)
    public void assertCanReadUser(SecurityUser actor, User target) {
        if (!canReadUsers(actor)) {
            throw new TranslatableAccessDeniedException("ACCESS_DENIED", "Access denied");
        }
        if (!organizationScopeService.canAccess(actor, target.getOrganization().getId())) {
            throw new TranslatableAccessDeniedException("ACCESS_DENIED_USER", "Access denied to this user");
        }
    }

    @Transactional(readOnly = true)
    public void assertCanWriteUser(SecurityUser actor, UserRequest request, User existing) {
        if (!canWriteUsers(actor)) {
            throw new TranslatableAccessDeniedException("ACCESS_DENIED", "Access denied");
        }
        assertCanAssignRole(actor, request.role(), existing);
        assertOrganizationWritable(actor, request.organizationId());
        if (existing != null) {
            assertCanReadUser(actor, existing);
        }
    }

    @Transactional(readOnly = true)
    public void assertCanManageUser(SecurityUser actor, User target) {
        if (!canWriteUsers(actor)) {
            throw new TranslatableAccessDeniedException("ACCESS_DENIED", "Access denied");
        }
        assertCanReadUser(actor, target);
        if (target.getRole() == UserRole.SUPER_ADMIN && actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new TranslatableAccessDeniedException(
                    "ACCESS_DENIED_SUPER_ADMIN_MANAGE", "Cannot manage SUPER_ADMIN accounts");
        }
    }

    public void assertCanAssignRole(SecurityUser actor, UserRole roleToAssign, User existing) {
        if (roleToAssign == UserRole.SUPER_ADMIN && actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new TranslatableAccessDeniedException(
                    "ACCESS_DENIED_SUPER_ADMIN_ASSIGN", "Cannot assign SUPER_ADMIN role");
        }
        if (existing != null
                && existing.getRole() == UserRole.SUPER_ADMIN
                && actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new TranslatableAccessDeniedException(
                    "ACCESS_DENIED_SUPER_ADMIN_MODIFY", "Cannot modify SUPER_ADMIN accounts");
        }
    }

    @Transactional(readOnly = true)
    public void assertOrganizationWritable(SecurityUser actor, UUID organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> AppException.badRequest("ORGANIZATION_NOT_FOUND", "Organization not found"));
        if (!org.isActive()) {
            throw AppException.badRequest("ORGANIZATION_INACTIVE", "Organization is inactive");
        }
        if (!organizationScopeService.canAccess(actor, organizationId)) {
            throw new TranslatableAccessDeniedException(
                    "ACCESS_DENIED_ORGANIZATION", "Access denied to this organization");
        }
    }

    @Transactional(readOnly = true)
    public Set<UUID> readableOrganizationIds(SecurityUser actor) {
        OrganizationScopeService.ScopeFilter filter = organizationScopeService.resolveScopeFilter(actor);
        return filter.organizationIds();
    }

    @Transactional(readOnly = true)
    public void assertCanAccessOrganization(SecurityUser actor, UUID organizationId) {
        if (!organizationScopeService.canAccess(actor, organizationId)) {
            throw new TranslatableAccessDeniedException(
                    "ACCESS_DENIED_ORGANIZATION_SCOPE", "Access denied to organization scope");
        }
    }

    @Transactional(readOnly = true)
    public void assertCanAccessFile(SecurityUser actor, FileEntity file) {
        if (organizationScopeService.hasGlobalScope(actor)) {
            return;
        }
        if (file.getCreatedBy() != null && Objects.equals(file.getCreatedBy().getId(), actor.getId())) {
            return;
        }
        if (filePassageRepository.existsByFileIdAndResponsibleUserId(file.getId(), actor.getId())) {
            return;
        }
        if (filePassageRepository.existsByFileIdAndSubstituteUserId(file.getId(), actor.getId())) {
            return;
        }
        if (file.getOrganization() != null
                && organizationScopeService.canAccess(actor, file.getOrganization().getId())) {
            return;
        }
        throw new TranslatableAccessDeniedException(
                "ACCESS_DENIED_ORGANIZATION_SCOPE", "Access denied to organization scope");
    }
}
