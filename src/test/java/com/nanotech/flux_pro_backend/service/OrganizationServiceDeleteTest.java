package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.OrganizationType;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceDeleteTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationTypeService organizationTypeService;

    @Mock
    private com.nanotech.flux_pro_backend.security.OrganizationScopeService organizationScopeService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrganizationService organizationService;

    @Test
    void delete_removesLeafOrganizationWithoutUsers() {
        UUID id = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(id);
        org.setCode("LEAF");
        org.setName("Leaf");
        org.setOrganizationType(new OrganizationType());

        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        when(organizationRepository.existsByParentId(id)).thenReturn(false);
        when(userRepository.existsByOrganizationId(id)).thenReturn(false);

        organizationService.delete(id);

        verify(organizationRepository).delete(org);
    }

    @Test
    void delete_rejectsWhenChildrenExist() {
        UUID id = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(id);

        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        when(organizationRepository.existsByParentId(id)).thenReturn(true);

        assertThatThrownBy(() -> organizationService.delete(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("child entities");

        verify(organizationRepository, never()).delete(org);
        verify(userRepository, never()).existsByOrganizationId(id);
    }

    @Test
    void delete_rejectsWhenUsersAssigned() {
        UUID id = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(id);

        when(organizationRepository.findById(id)).thenReturn(Optional.of(org));
        when(organizationRepository.existsByParentId(id)).thenReturn(false);
        when(userRepository.existsByOrganizationId(id)).thenReturn(true);

        assertThatThrownBy(() -> organizationService.delete(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("assigned users");

        verify(organizationRepository, never()).delete(org);
    }

    @Test
    void delete_rejectsWhenOrganizationNotFound() {
        UUID id = UUID.randomUUID();
        when(organizationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.delete(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");

        verify(organizationRepository, never()).delete(any(Organization.class));
    }
}
