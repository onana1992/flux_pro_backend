package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
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

    /**
     * Transmettre / retourner / suspendre : uniquement le responsable du maillon,
     * ou SUPER_ADMIN / BUSINESS_ADMIN. Les autres rôles (même hiérarchiquement
     * supérieurs) ne peuvent pas agir à la place du responsable.
     */
    public boolean canActOnPassage(SecurityUser actor, FilePassage passage) {
        if (actor.getRole() == UserRole.SUPER_ADMIN || actor.getRole() == UserRole.BUSINESS_ADMIN) {
            return true;
        }
        User responsible = passage.getResponsibleUser();
        return responsible != null && actor.getId().equals(responsible.getId());
    }

    /** Rang relatif des rôles — réutilisé pour l'escalade d'alertes (ALR). */
    public boolean isHierarchicallySuperior(UserRole actorRole, UserRole responsibleRole) {
        return ROLE_RANK.getOrDefault(actorRole, 0) > ROLE_RANK.getOrDefault(responsibleRole, 0);
    }
}
