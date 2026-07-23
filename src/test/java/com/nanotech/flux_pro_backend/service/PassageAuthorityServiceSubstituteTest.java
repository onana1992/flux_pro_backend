package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.security.RbacAuthorityService;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassageAuthorityServiceSubstituteTest {

    @Mock
    private SubstituteService substituteService;

    @InjectMocks
    private PassageAuthorityService passageAuthorityService;

    @Test
    void canActOnPassage_allowsActiveSubstitute() {
        User responsible = user(UUID.randomUUID(), UserRole.AGENT);
        User substitute = user(UUID.randomUUID(), UserRole.AGENT);
        SecurityUser actor = securityUser(substitute);

        FilePassage passage = new FilePassage();
        passage.setResponsibleUser(responsible);

        when(substituteService.isActiveSubstituteOf(substitute.getId(), responsible)).thenReturn(true);

        assertThat(passageAuthorityService.canActOnPassage(actor, passage)).isTrue();
    }

    @Test
    void canActOnPassage_deniesUnrelatedUser() {
        User responsible = user(UUID.randomUUID(), UserRole.AGENT);
        User other = user(UUID.randomUUID(), UserRole.AGENT);
        SecurityUser actor = securityUser(other);

        FilePassage passage = new FilePassage();
        passage.setResponsibleUser(responsible);

        when(substituteService.isActiveSubstituteOf(other.getId(), responsible)).thenReturn(false);

        assertThat(passageAuthorityService.canActOnPassage(actor, passage)).isFalse();
    }

    private static User user(UUID id, UserRole role) {
        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setCode("ORG");
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@mintp.cm");
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setOrganization(org);
        user.setActive(true);
        return user;
    }

    private static SecurityUser securityUser(User user) {
        return new SecurityUser(
                user, new RbacAuthorityService.RbacAuthorities(List.of(user.getRole().name()), List.of()));
    }
}
