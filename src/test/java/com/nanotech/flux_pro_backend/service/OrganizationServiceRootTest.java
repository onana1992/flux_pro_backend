package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.dto.request.OrganizationRequest;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.OrganizationType;
import com.nanotech.flux_pro_backend.repository.FileNumberSequenceRepository;
import com.nanotech.flux_pro_backend.repository.FileRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import com.nanotech.flux_pro_backend.security.OrganizationScopeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceRootTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationTypeService organizationTypeService;

    @Mock
    private OrganizationScopeService organizationScopeService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileNumberSequenceRepository fileNumberSequenceRepository;

    @InjectMocks
    private OrganizationService organizationService;

    @Test
    void create_rejectsSecondRootOrganization() {
        UUID typeId = UUID.randomUUID();
        OrganizationType ministry = ministryType(typeId);

        when(organizationRepository.existsByCode("OTHER")).thenReturn(false);
        when(organizationTypeService.getById(typeId)).thenReturn(ministry);
        when(organizationRepository.existsOtherRoot(null)).thenReturn(true);

        OrganizationRequest request = new OrganizationRequest("OTHER", "Autre ministère", typeId, null, true);

        assertThatThrownBy(() -> organizationService.create(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("root organization already exists");

        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void create_allowsFirstRootOrganization() {
        UUID typeId = UUID.randomUUID();
        OrganizationType ministry = ministryType(typeId);

        when(organizationRepository.existsByCode("MINTP")).thenReturn(false);
        when(organizationTypeService.getById(typeId)).thenReturn(ministry);
        when(organizationRepository.existsOtherRoot(null)).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationRequest request = new OrganizationRequest("MINTP", "Ministère", typeId, null, true);
        Organization saved = organizationService.create(request);

        assertThat(saved.getCode()).isEqualTo("MINTP");
        assertThat(saved.getParent()).isNull();
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void update_allowsEditingExistingRoot() {
        UUID id = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();
        OrganizationType ministry = ministryType(typeId);

        Organization root = new Organization();
        root.setId(id);
        root.setCode("MINTP");
        root.setName("Ancien nom");
        root.setOrganizationType(ministry);
        root.setParent(null);

        when(organizationRepository.findById(id)).thenReturn(Optional.of(root));
        when(organizationTypeService.getById(typeId)).thenReturn(ministry);
        when(organizationRepository.existsOtherRoot(id)).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenAnswer(inv -> inv.getArgument(0));

        OrganizationRequest request = new OrganizationRequest("MINTP", "Nouveau nom", typeId, null, true);
        Organization updated = organizationService.update(id, request);

        assertThat(updated.getName()).isEqualTo("Nouveau nom");
        assertThat(updated.getParent()).isNull();
    }

    @Test
    void update_rejectsPromotingChildToSecondRoot() {
        UUID id = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();
        OrganizationType ministry = ministryType(typeId);

        Organization parent = new Organization();
        parent.setId(UUID.randomUUID());
        parent.setCode("MINTP");

        Organization child = new Organization();
        child.setId(id);
        child.setCode("DSI");
        child.setName("DSI");
        child.setOrganizationType(ministry);
        child.setParent(parent);

        when(organizationRepository.findById(id)).thenReturn(Optional.of(child));
        when(organizationTypeService.getById(typeId)).thenReturn(ministry);
        when(organizationRepository.existsOtherRoot(id)).thenReturn(true);

        OrganizationRequest request = new OrganizationRequest("DSI", "DSI", typeId, null, true);

        assertThatThrownBy(() -> organizationService.update(id, request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("root organization already exists");

        verify(organizationRepository, never()).save(any(Organization.class));
    }

    private static OrganizationType ministryType(UUID typeId) {
        OrganizationType type = new OrganizationType();
        type.setId(typeId);
        type.setCode("MINISTRY");
        type.setActive(true);
        type.setAllowsRoot(true);
        return type;
    }
}
