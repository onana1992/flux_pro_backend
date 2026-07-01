package com.nanotech.flux_pro_backend.security;

import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.OrganizationType;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationScopeServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private OrganizationScopeService organizationScopeService;

    private Organization mintp;
    private Organization drtpC;
    private Organization drtpLittoral;
    private Organization drtpCService;

    @BeforeEach
    void setUp() {
        OrganizationType ministry = type("MINISTRY", false);
        OrganizationType regional = type("REGIONAL_DIRECTORATE", true);
        OrganizationType service = type("SERVICE", false);

        mintp = org(UUID.randomUUID(), "MINTP", ministry, null);
        drtpC = org(UUID.randomUUID(), "DRTP-C", regional, mintp);
        drtpLittoral = org(UUID.randomUUID(), "DRTP-LITTORAL", regional, mintp);
        drtpCService = org(UUID.randomUUID(), "DRTP-C-AUTH", service, drtpC);
    }

    @Test
    void regionalDirector_canAccessOnlySameDrtpBranch() {
        SecurityUser director = securityUser(UserRole.REGIONAL_DIRECTOR, drtpCService);

        when(organizationRepository.findById(director.getOrganizationId())).thenReturn(Optional.of(drtpCService));
        when(organizationRepository.findById(drtpC.getId())).thenReturn(Optional.of(drtpC));
        when(organizationRepository.findById(drtpLittoral.getId())).thenReturn(Optional.of(drtpLittoral));
        when(organizationRepository.findById(drtpCService.getId())).thenReturn(Optional.of(drtpCService));

        assertTrue(organizationScopeService.canAccess(director, drtpCService.getId()));
        assertTrue(organizationScopeService.canAccess(director, drtpC.getId()));
        assertFalse(organizationScopeService.canAccess(director, drtpLittoral.getId()));
    }

    @Test
    void superAdmin_hasGlobalScope() {
        SecurityUser admin = securityUser(UserRole.SUPER_ADMIN, drtpCService);

        assertTrue(organizationScopeService.hasGlobalScope(admin));
        assertTrue(organizationScopeService.canAccess(admin, drtpLittoral.getId()));
    }

    @Test
    void resolveScopeFilter_forRegionalDirector_returnsBranchIds() {
        SecurityUser director = securityUser(UserRole.REGIONAL_DIRECTOR, drtpCService);

        when(organizationRepository.findById(director.getOrganizationId())).thenReturn(Optional.of(drtpCService));
        when(organizationRepository.findAllActive()).thenReturn(List.of(mintp, drtpC, drtpLittoral, drtpCService));

        var filter = organizationScopeService.resolveScopeFilter(director);

        assertFalse(filter.allOrganizations());
        assertTrue(filter.organizationIds().contains(drtpC.getId()));
        assertTrue(filter.organizationIds().contains(drtpCService.getId()));
        assertFalse(filter.organizationIds().contains(drtpLittoral.getId()));
    }

    private static OrganizationType type(String code, boolean regionalScope) {
        OrganizationType type = new OrganizationType();
        type.setId(UUID.randomUUID());
        type.setCode(code);
        type.setName(code);
        type.setRegionalScope(regionalScope);
        type.setActive(true);
        return type;
    }

    private static Organization org(UUID id, String code, OrganizationType type, Organization parent) {
        Organization org = new Organization();
        org.setId(id);
        org.setCode(code);
        org.setName(code);
        org.setOrganizationType(type);
        org.setParent(parent);
        org.setActive(true);
        return org;
    }

    private static SecurityUser securityUser(UserRole role, Organization organization) {
        com.nanotech.flux_pro_backend.entity.User user = new com.nanotech.flux_pro_backend.entity.User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@mintp.cm");
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setOrganization(organization);
        user.setStaffNumber("MAT-TEST");
        user.setLastName("TEST");
        user.setFirstName("User");
        user.setActive(true);
        user.setMustChangePassword(false);
        return new SecurityUser(user);
    }
}
