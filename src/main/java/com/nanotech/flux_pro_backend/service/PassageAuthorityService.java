package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.security.OrganizationScopeService;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class PassageAuthorityService {

    private static final Map<UserRole, Integer> ROLE_RANK = new EnumMap<>(UserRole.class);

    static {
        ROLE_RANK.put(UserRole.READER, 0);
        ROLE_RANK.put(UserRole.SUPPORT, 10);
        ROLE_RANK.put(UserRole.AGENT, 10);
        ROLE_RANK.put(UserRole.SERVICE_HEAD, 20);
        ROLE_RANK.put(UserRole.REGIONAL_DIRECTOR, 30);
        ROLE_RANK.put(UserRole.DIRECTOR, 40);
        ROLE_RANK.put(UserRole.SECRETARY_GENERAL, 50);
        ROLE_RANK.put(UserRole.EXECUTIVE_OFFICE, 60);
        ROLE_RANK.put(UserRole.BUSINESS_ADMIN, 70);
        ROLE_RANK.put(UserRole.SUPER_ADMIN, 80);
    }

    private final OrganizationScopeService organizationScopeService;

    public PassageAuthorityService(OrganizationScopeService organizationScopeService) {
        this.organizationScopeService = organizationScopeService;
    }

    public boolean canActOnPassage(SecurityUser actor, FilePassage passage) {
        User responsible = passage.getResponsibleUser();
        if (responsible == null) {
            return organizationScopeService.hasGlobalScope(actor);
        }
        if (actor.getId().equals(responsible.getId())) {
            return true;
        }
        if (organizationScopeService.hasGlobalScope(actor)) {
            return true;
        }
        return isHierarchicallySuperior(actor.getRole(), responsible.getRole());
    }

    public boolean isHierarchicallySuperior(UserRole actorRole, UserRole responsibleRole) {
        return ROLE_RANK.getOrDefault(actorRole, 0) > ROLE_RANK.getOrDefault(responsibleRole, 0);
    }
}
