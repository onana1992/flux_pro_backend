package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.enumeration.OrganizationType;
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

    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public boolean canAccess(SecurityUser user, UUID targetOrgId) {
        if (user.getRole() == UserRole.SUPER_ADMIN
                || user.getRole() == UserRole.SECRETARY_GENERAL
                || user.getRole() == UserRole.EXECUTIVE_OFFICE) {
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
            String userRoot = findDrtpRootCode(userOrg);
            String targetRoot = findDrtpRootCode(target);
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

    private String findDrtpRootCode(Organization org) {
        Organization current = org;
        while (current != null) {
            if (current.getType() == OrganizationType.REGIONAL_DIRECTORATE) {
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
