package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.AlertRule;
import com.nanotech.flux_pro_backend.entity.AlertType;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.enumeration.AlertTargetMode;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.AlertRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertRuleSeedProfileServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;
    @Mock
    private AlertTypeService alertTypeService;

    @InjectMocks
    private AlertRuleSeedProfileService alertRuleSeedProfileService;

    private ChainTemplate template;

    @BeforeEach
    void setUp() {
        template = new ChainTemplate();
        template.setId(UUID.randomUUID());
        when(alertTypeService.getByCode(anyString())).thenAnswer(invocation -> {
            AlertType type = new AlertType();
            type.setId(UUID.randomUUID());
            type.setCode(invocation.getArgument(0));
            return type;
        });
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void apply_createsAllProfileRows_whenTemplateHasNoExistingRules() {
        when(alertRuleRepository.existsByChainTemplateIdAndThresholdCodeAndTargetModeAndTargetRole(
                eq(template.getId()), anyString(), any(), any())).thenReturn(false);

        List<AlertRule> created = alertRuleSeedProfileService.apply(template, false);

        assertThat(created).hasSize(6);
        verify(alertRuleRepository, never()).deleteAllByChainTemplateId(any());
        assertThat(created).extracting(AlertRule::getChainTemplate).containsOnly(template);
    }

    @Test
    void apply_skipsRowsAlreadyPresent_whenNotOverwriting() {
        when(alertRuleRepository.existsByChainTemplateIdAndThresholdCodeAndTargetModeAndTargetRole(
                eq(template.getId()), anyString(), any(), any())).thenAnswer(invocation -> {
            String threshold = invocation.getArgument(1);
            return "J_MINUS_2".equals(threshold);
        });

        List<AlertRule> created = alertRuleSeedProfileService.apply(template, false);

        assertThat(created).hasSize(5);
        assertThat(created).noneMatch(r -> "J_MINUS_2".equals(r.getThresholdCode()));
    }

    @Test
    void apply_deletesExistingRulesFirst_whenOverwriteRequested() {
        when(alertRuleRepository.existsByChainTemplateIdAndThresholdCodeAndTargetModeAndTargetRole(
                eq(template.getId()), anyString(), any(), any())).thenReturn(false);

        alertRuleSeedProfileService.apply(template, true);

        verify(alertRuleRepository, times(1)).deleteAllByChainTemplateId(template.getId());
    }

    @Test
    void apply_escalationRow_targetsDirectorAtLevelOne() {
        when(alertRuleRepository.existsByChainTemplateIdAndThresholdCodeAndTargetModeAndTargetRole(
                eq(template.getId()), anyString(), any(), any())).thenReturn(false);

        List<AlertRule> created = alertRuleSeedProfileService.apply(template, false);

        assertThat(created).anySatisfy(rule -> {
            if ("J_PLUS_3".equals(rule.getThresholdCode())) {
                assertThat(rule.getTargetRole()).isEqualTo(UserRole.DIRECTOR);
                assertThat(rule.getEscalationLevel()).isEqualTo(1);
            }
        });
    }
}
