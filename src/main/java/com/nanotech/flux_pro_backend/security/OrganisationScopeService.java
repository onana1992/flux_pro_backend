package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.organisation.Organisation;
import com.nanotech.flux_pro_backend.organisation.OrganisationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganisationScopeService {

    private final OrganisationRepository organisationRepository;

    @Transactional(readOnly = true)
    public boolean canAccess(SecurityUser user, UUID targetOrgId) {
        if (user.getRole() == UserRole.SUPER_ADMIN
                || user.getRole() == UserRole.SG
                || user.getRole() == UserRole.CABINET) {
            return true;
        }

        Organisation target = organisationRepository.findById(targetOrgId).orElse(null);
        if (target == null) {
            return false;
        }

        Organisation userOrg = organisationRepository.findById(user.getOrganisationId()).orElse(null);
        if (userOrg == null) {
            return false;
        }

        if (user.getRole() == UserRole.DRTP) {
            String userRoot = findDrtpRootCode(userOrg);
            String targetRoot = findDrtpRootCode(target);
            return userRoot != null && userRoot.equals(targetRoot);
        }

        Set<UUID> scope = collectSubtree(userOrg.getId());
        scope.add(userOrg.getId());
        return scope.contains(targetOrgId) || isSameBranch(userOrg, target);
    }

    private boolean isSameBranch(Organisation userOrg, Organisation target) {
        Set<UUID> userAncestors = collectAncestors(userOrg);
        return userAncestors.contains(target.getId()) || collectAncestors(target).contains(userOrg.getId());
    }

    private String findDrtpRootCode(Organisation org) {
        Organisation current = org;
        while (current != null) {
            if (current.getType() == com.nanotech.flux_pro_backend.organisation.OrganisationType.DRTP) {
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
        List<Organisation> children = organisationRepository.findByParentId(parentId);
        for (Organisation child : children) {
            ids.add(child.getId());
            collectChildren(child.getId(), ids);
        }
    }

    private Set<UUID> collectAncestors(Organisation org) {
        Set<UUID> ids = new HashSet<>();
        Organisation current = org.getParent();
        while (current != null) {
            ids.add(current.getId());
            current = current.getParent();
        }
        return ids;
    }
}
