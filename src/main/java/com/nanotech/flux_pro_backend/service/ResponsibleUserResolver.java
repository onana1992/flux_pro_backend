package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResponsibleUserResolver {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public User resolve(FileEntity file, UserRole role) {
        List<UUID> orgIds = collectOrganizationChain(file.getOrganization());
        List<User> candidates = userRepository.findActiveByRoleInOrganizations(role, orgIds);
        if (candidates.isEmpty()) {
            return null;
        }
        UUID fileOrgId = file.getOrganization().getId();
        return candidates.stream()
                .filter(u -> u.getOrganization().getId().equals(fileOrgId))
                .findFirst()
                .orElse(candidates.get(0));
    }

    private List<UUID> collectOrganizationChain(Organization organization) {
        Set<UUID> ids = new LinkedHashSet<>();
        UUID currentId = organization.getId();
        while (currentId != null) {
            ids.add(currentId);
            Organization current = organizationRepository.findById(currentId).orElse(null);
            if (current == null || current.getParent() == null) {
                break;
            }
            currentId = current.getParent().getId();
        }
        return new ArrayList<>(ids);
    }
}
