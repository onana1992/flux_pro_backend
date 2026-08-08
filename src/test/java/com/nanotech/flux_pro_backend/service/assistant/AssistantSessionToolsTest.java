package com.nanotech.flux_pro_backend.service.assistant;

import com.nanotech.flux_pro_backend.dto.response.UserProfileResponse;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.entity.OrganizationType;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.security.RbacAuthorityService;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import com.nanotech.flux_pro_backend.service.DashboardService;
import com.nanotech.flux_pro_backend.service.FileAttachmentService;
import com.nanotech.flux_pro_backend.service.FileService;
import com.nanotech.flux_pro_backend.service.NotificationService;
import com.nanotech.flux_pro_backend.service.PassageService;
import com.nanotech.flux_pro_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantSessionToolsTest {

    @Mock private UserService userService;
    @Mock private FileService fileService;
    @Mock private PassageService passageService;
    @Mock private FileAttachmentService fileAttachmentService;
    @Mock private NotificationService notificationService;
    @Mock private DashboardService dashboardService;

    private SecurityUser actor;
    private AssistantSessionTools tools;

    @BeforeEach
    void setUp() {
        actor = securityUser(List.of(RbacPermissions.FILES_READ));
        tools = AssistantSessionTools.forUser(
                actor,
                userService,
                fileService,
                passageService,
                fileAttachmentService,
                notificationService,
                dashboardService,
                2);
    }

    @Test
    void getCurrentUser_returnsProfileJson() {
        when(userService.getMeProfile(actor)).thenReturn(new UserProfileResponse(
                actor.getId(),
                "agent@mintp.cm",
                "Dupont",
                "Ada",
                UserRole.AGENT,
                null,
                false,
                List.of("AGENT"),
                List.of(RbacPermissions.FILES_READ)));

        String json = tools.getCurrentUser();

        assertThat(json).contains("agent@mintp.cm").contains("AGENT");
        assertThat(tools.getToolsUsed()).containsExactly("get_current_user");
    }

    @Test
    void searchFiles_withoutPermission_doesNotCallService() {
        actor = securityUser(List.of());
        tools = AssistantSessionTools.forUser(
                actor,
                userService,
                fileService,
                passageService,
                fileAttachmentService,
                notificationService,
                dashboardService,
                5);

        String json = tools.searchFiles(null, null, null, null, null, null, null, null);

        assertThat(json).contains("PERMISSION_DENIED");
        verify(fileService, never()).findAll(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void enforcesMaxToolCalls() {
        when(userService.getMeProfile(actor)).thenReturn(new UserProfileResponse(
                actor.getId(),
                "agent@mintp.cm",
                "Dupont",
                "Ada",
                UserRole.AGENT,
                null,
                false,
                List.of("AGENT"),
                List.of()));

        tools.getCurrentUser();
        tools.getCurrentUser();
        String limited = tools.getCurrentUser();

        assertThat(limited).contains("TOOL_CALL_LIMIT");
        assertThat(tools.getToolsUsed()).hasSize(3);
    }

    private static SecurityUser securityUser(List<String> permissions) {
        OrganizationType type = new OrganizationType();
        type.setId(UUID.randomUUID());
        type.setCode("DIR");
        type.setName("Direction");
        type.setActive(true);

        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setCode("DGI");
        org.setName("DGI");
        org.setOrganizationType(type);
        org.setActive(true);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("agent@mintp.cm");
        user.setPasswordHash("hash");
        user.setRole(UserRole.AGENT);
        user.setOrganization(org);
        user.setStaffNumber("MAT-1");
        user.setLastName("Dupont");
        user.setFirstName("Ada");
        user.setActive(true);
        user.setMustChangePassword(false);

        return new SecurityUser(
                user,
                new RbacAuthorityService.RbacAuthorities(List.of("AGENT"), permissions));
    }
}
