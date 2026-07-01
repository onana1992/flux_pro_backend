package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationScopeService {

    public record ScopeFilter(boolean allOrganizations, Set<UUID> organizationIds) {
    }

    private final OrganizationRepository organizationRepository;

    public boolean hasGlobalScope(SecurityUser user) {
        return user.getRole() == UserRole.SUPER_ADMIN
                || user.getRole() == UserRole.SECRETARY_GENERAL
                || user.getRole() == UserRole.EXECUTIVE_OFFICE;
    }

    @Transactional(readOnly = true)
    public ScopeFilter resolveScopeFilter(SecurityUser user) {
        if (hasGlobalScope(user)) {
            return new ScopeFilter(true, Set.of());
        }

        Organization userOrg = organizationRepository.findById(user.getOrganizationId()).orElse(null);
        if (userOrg == null) {
            return new ScopeFilter(false, Set.of());
        }

        if (user.getRole() == UserRole.REGIONAL_DIRECTOR) {
            String rootCode = findRegionalRootCode(userOrg);
            if (rootCode == null) {
                return new ScopeFilter(false, Set.of(userOrg.getId()));
            }
            Set<UUID> ids = new HashSet<>();
            for (Organization org : organizationRepository.findAllActive()) {
                if (rootCode.equals(findRegionalRootCode(org))) {
                    ids.add(org.getId());
                }
            }
            return new ScopeFilter(false, ids);
        }

        Set<UUID> scope = collectSubtree(userOrg.getId());
        scope.add(userOrg.getId());
        return new ScopeFilter(false, scope);
    }

    @Transactional(readOnly = true)
    public boolean canAccess(SecurityUser user, UUID targetOrgId) {
        if (hasGlobalScope(user)) {
            return true;
        }

        Organization target = organizationRepository.findById(targetOrgId).orElse(null);
        if (target == null) {
            return false;
        }

        Organization userOrg = organizationRepository.findById(user.getOrganizationId()).orElse(null);
        if (userOrg == null) {
            return false;
        }

        if (user.getRole() == UserRole.REGIONAL_DIRECTOR) {
            String userRoot = findRegionalRootCode(userOrg);
            String targetRoot = findRegionalRootCode(target);
            return userRoot != null && userRoot.equals(targetRoot);
        }

        Set<UUID> scope = collectSubtree(userOrg.getId());
        scope.add(userOrg.getId());
        return scope.contains(targetOrgId) || isSameBranch(userOrg, target);
    }

    private boolean isSameBranch(Organization userOrg, Organization target) {
        Set<UUID> userAncestors = collectAncestors(userOrg);
        return userAncestors.contains(target.getId()) || collectAncestors(target).contains(userOrg.getId());
    }

    private String findRegionalRootCode(Organization org) {
        Organization current = org;
        while (current != null) {
            if (current.getOrganizationType() != null && current.getOrganizationType().isRegionalScope()) {
                return current.getCode();
            }
            current = current.getParent();
        }
        return null;
    }

    private Set<UUID> collectSubtree(UUID rootId) {
        Set<UUID> ids = new HashSet<>();
        collectChildren(rootId, ids);
        return ids;
    }

    private void collectChildren(UUID parentId, Set<UUID> ids) {
        List<Organization> children = organizationRepository.findByParentId(parentId);
        for (Organization child : children) {
            ids.add(child.getId());
            collectChildren(child.getId(), ids);
        }
    }

    private Set<UUID> collectAncestors(Organization org) {
        Set<UUID> ids = new HashSet<>();
        Organization current = org.getParent();
        while (current != null) {
            ids.add(current.getId());
            current = current.getParent();
        }
        return ids;
    }
}
