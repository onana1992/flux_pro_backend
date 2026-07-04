package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AlertException;
import com.nanotech.flux_pro_backend.dto.request.AlertRuleRequest;
import com.nanotech.flux_pro_backend.entity.AlertRule;
import com.nanotech.flux_pro_backend.entity.AlertType;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.enumeration.AlertTargetMode;
import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.AlertRuleRepository;
import com.nanotech.flux_pro_backend.repository.ChainStepTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertRuleServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;
    @Mock
    private ChainStepTemplateRepository chainStepTemplateRepository;
    @Mock
    private ChainTemplateService chainTemplateService;
    @Mock
    private AlertTypeService alertTypeService;
    @Mock
    private AlertRuleSeedProfileService alertRuleSeedProfileService;

    @InjectMocks
    private AlertRuleService alertRuleService;

    private ChainTemplate template;
    private AlertType alertType;

    @BeforeEach
    void setUp() {
        template = new ChainTemplate();
        template.setId(UUID.randomUUID());

        alertType = new AlertType();
        alertType.setId(UUID.randomUUID());
        alertType.setCode("OVERDUE");
    }

    @Test
    void create_rejectsRoleModeWithoutTargetRole() {
        when(chainTemplateService.findById(template.getId())).thenReturn(template);
        AlertRuleRequest request = new AlertRuleRequest(
                null, "J_PLUS_0", 0, DelayUnit.WORKING_DAYS, alertType.getId(), null,
                AlertTargetMode.ROLE, null, null, true);

        assertThatThrownBy(() -> alertRuleService.create(template.getId(), request))
                .isInstanceOf(AlertException.class)
                .hasMessageContaining("targetRole");
    }

    @Test
    void create_rejectsStepFromAnotherTemplate() {
        when(chainTemplateService.findById(template.getId())).thenReturn(template);
        when(alertTypeService.getById(alertType.getId())).thenReturn(alertType);
        UUID stepId = UUID.randomUUID();
        when(chainStepTemplateRepository.findByIdAndChainTemplateId(stepId, template.getId()))
                .thenReturn(Optional.empty());

        AlertRuleRequest request = new AlertRuleRequest(
                stepId, "J_PLUS_0", 0, DelayUnit.WORKING_DAYS, alertType.getId(), null,
                AlertTargetMode.CURRENT_RESPONSIBLE, null, null, true);

        assertThatThrownBy(() -> alertRuleService.create(template.getId(), request))
                .isInstanceOf(AlertException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void create_succeeds_withCurrentResponsibleMode() {
        when(chainTemplateService.findById(template.getId())).thenReturn(template);
        when(alertTypeService.getById(alertType.getId())).thenReturn(alertType);
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(inv -> inv.getArgument(0));

        AlertRuleRequest request = new AlertRuleRequest(
                null, "j_minus_2", -2, DelayUnit.WORKING_DAYS, alertType.getId(), null,
                AlertTargetMode.CURRENT_RESPONSIBLE, UserRole.DIRECTOR, null, true);

        AlertRule created = alertRuleService.create(template.getId(), request);

        assertThat(created.getThresholdCode()).isEqualTo("J_MINUS_2");
        assertThat(created.getTargetRole()).isNull();
        assertThat(created.getChainTemplate()).isSameAs(template);
    }

    @Test
    void create_normalizesPriorityScope() {
        when(chainTemplateService.findById(template.getId())).thenReturn(template);
        when(alertTypeService.getById(alertType.getId())).thenReturn(alertType);
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(inv -> inv.getArgument(0));

        AlertRuleRequest request = new AlertRuleRequest(
                null, "J_PLUS_15", 15, DelayUnit.WORKING_DAYS, alertType.getId(), 3,
                AlertTargetMode.ROLE, UserRole.EXECUTIVE_OFFICE, "urgent_plus", true);

        AlertRule created = alertRuleService.create(template.getId(), request);

        assertThat(created.getPriorityScope()).isEqualTo("URGENT_PLUS");
        assertThat(created.getTargetRole()).isEqualTo(UserRole.EXECUTIVE_OFFICE);
    }

    @Test
    void applyDefaultProfile_delegatesToSeedProfileService() {
        when(chainTemplateService.findById(template.getId())).thenReturn(template);

        alertRuleService.applyDefaultProfile(template.getId(), true);

        org.mockito.Mockito.verify(alertRuleSeedProfileService).apply(template, true);
    }
}
