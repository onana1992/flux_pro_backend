package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.Alert;
import com.nanotech.flux_pro_backend.entity.AlertRule;
import com.nanotech.flux_pro_backend.entity.AlertType;
import com.nanotech.flux_pro_backend.entity.ChainStepTemplate;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.AlertChannel;
import com.nanotech.flux_pro_backend.enumeration.AlertTargetMode;
import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.FilePriority;
import com.nanotech.flux_pro_backend.enumeration.FileStatus;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.AlertRepository;
import com.nanotech.flux_pro_backend.repository.AlertRuleRepository;
import com.nanotech.flux_pro_backend.repository.FilePassageRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertEngineServiceTest {

    @Mock
    private FilePassageRepository filePassageRepository;
    @Mock
    private AlertRuleRepository alertRuleRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private DelaiService delaiService;
    @Mock
    private ClockService clockService;
    @Mock
    private ResponsibleUserResolver responsibleUserResolver;
    @Mock
    private NotificationService notificationService;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private AlertEngineService alertEngineService;

    private ChainTemplate template;
    private ChainStepTemplate stepTemplate;
    private FileEntity file;
    private FilePassage passage;
    private AlertType alertType;
    private User responsible;

    @BeforeEach
    void setUp() {
        template = new ChainTemplate();
        template.setId(UUID.randomUUID());

        stepTemplate = new ChainStepTemplate();
        stepTemplate.setId(UUID.randomUUID());
        stepTemplate.setChainTemplate(template);
        stepTemplate.setLabel("Instruction");

        file = new FileEntity();
        file.setId(UUID.randomUUID());
        file.setChainTemplate(template);
        file.setStatus(FileStatus.IN_PROGRESS);
        file.setPriority(FilePriority.NORMAL);

        responsible = new User();
        responsible.setId(UUID.randomUUID());

        passage = new FilePassage();
        passage.setId(UUID.randomUUID());
        passage.setFile(file);
        passage.setChainStepTemplate(stepTemplate);
        passage.setResponsibleUser(responsible);
        passage.setDueAt(Instant.now());

        alertType = new AlertType();
        alertType.setId(UUID.randomUUID());
        alertType.setCode("OVERDUE");

        org.mockito.Mockito.lenient().when(notificationService.activeChannels())
                .thenReturn(List.of(AlertChannel.IN_APP, AlertChannel.EMAIL));
        org.mockito.Mockito.lenient().when(alertRepository.save(any(Alert.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(clockService.now()).thenAnswer(inv -> Instant.now());
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        lenient().doNothing().when(transactionManager).commit(any());
        lenient().doNothing().when(transactionManager).rollback(any());
        lenient().when(filePassageRepository.findById(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return id.equals(passage.getId()) ? Optional.of(passage) : Optional.empty();
        });
    }

    private AlertRule ruleCurrentResponsible(Instant now, int offsetValue) {
        AlertRule rule = new AlertRule();
        rule.setId(UUID.randomUUID());
        rule.setChainTemplate(template);
        rule.setAlertType(alertType);
        rule.setOffsetValue(offsetValue);
        rule.setOffsetUnit(DelayUnit.WORKING_DAYS);
        rule.setTargetMode(AlertTargetMode.CURRENT_RESPONSIBLE);
        rule.setActive(true);
        return rule;
    }

    @Test
    void evaluateAll_triggersAlertOnBothChannels_whenThresholdReached() {
        Instant now = Instant.now();
        AlertRule rule = ruleCurrentResponsible(now, 0);
        when(filePassageRepository.findActiveCandidatesForAlerts()).thenReturn(List.of(passage));
        when(alertRuleRepository.findByChainTemplateIdAndActiveTrue(template.getId())).thenReturn(List.of(rule));
        when(delaiService.applyOffset(passage.getDueAt(), 0, DelayUnit.WORKING_DAYS)).thenReturn(passage.getDueAt());
        when(alertRepository.existsByFilePassageIdAndAlertRuleIdAndChannel(any(), any(), any())).thenReturn(false);

        alertEngineService.evaluateAll();

        verify(notificationService, times(2)).dispatch(any(Alert.class));
        verify(alertRepository, times(2)).save(any(Alert.class));
    }

    @Test
    void evaluateAll_skipsAlert_whenThresholdNotReachedYet() {
        Instant now = Instant.now();
        Instant future = now.plus(1, ChronoUnit.DAYS);
        AlertRule rule = ruleCurrentResponsible(now, -2);
        when(filePassageRepository.findActiveCandidatesForAlerts()).thenReturn(List.of(passage));
        when(alertRuleRepository.findByChainTemplateIdAndActiveTrue(template.getId())).thenReturn(List.of(rule));
        when(delaiService.applyOffset(passage.getDueAt(), -2, DelayUnit.WORKING_DAYS)).thenReturn(future);

        alertEngineService.evaluateAll();

        verify(notificationService, never()).dispatch(any(Alert.class));
    }

    @Test
    void evaluateAll_isIdempotent_whenAlertAlreadySentOnChannel() {
        Instant now = Instant.now();
        AlertRule rule = ruleCurrentResponsible(now, 0);
        when(filePassageRepository.findActiveCandidatesForAlerts()).thenReturn(List.of(passage));
        when(alertRuleRepository.findByChainTemplateIdAndActiveTrue(template.getId())).thenReturn(List.of(rule));
        when(delaiService.applyOffset(passage.getDueAt(), 0, DelayUnit.WORKING_DAYS)).thenReturn(passage.getDueAt());
        when(alertRepository.existsByFilePassageIdAndAlertRuleIdAndChannel(
                passage.getId(), rule.getId(), AlertChannel.IN_APP)).thenReturn(true);
        when(alertRepository.existsByFilePassageIdAndAlertRuleIdAndChannel(
                passage.getId(), rule.getId(), AlertChannel.EMAIL)).thenReturn(false);

        alertEngineService.evaluateAll();

        verify(notificationService, times(1)).dispatch(any(Alert.class));
    }

    @Test
    void evaluateAll_resolvesRoleBasedRecipient_viaResponsibleUserResolver() {
        Instant now = Instant.now();
        AlertRule rule = new AlertRule();
        rule.setId(UUID.randomUUID());
        rule.setChainTemplate(template);
        rule.setAlertType(alertType);
        rule.setOffsetValue(3);
        rule.setOffsetUnit(DelayUnit.WORKING_DAYS);
        rule.setTargetMode(AlertTargetMode.ROLE);
        rule.setTargetRole(UserRole.DIRECTOR);
        rule.setActive(true);

        User director = new User();
        director.setId(UUID.randomUUID());

        when(filePassageRepository.findActiveCandidatesForAlerts()).thenReturn(List.of(passage));
        when(alertRuleRepository.findByChainTemplateIdAndActiveTrue(template.getId())).thenReturn(List.of(rule));
        when(delaiService.applyOffset(passage.getDueAt(), 3, DelayUnit.WORKING_DAYS)).thenReturn(passage.getDueAt());
        when(responsibleUserResolver.resolve(file, UserRole.DIRECTOR)).thenReturn(director);

        alertEngineService.evaluateAll();

        verify(responsibleUserResolver).resolve(file, UserRole.DIRECTOR);
        verify(notificationService, times(2)).dispatch(any(Alert.class));
    }

    @Test
    void evaluateAll_skipsRule_whenRoleRecipientCannotBeResolved() {
        Instant now = Instant.now();
        AlertRule rule = new AlertRule();
        rule.setId(UUID.randomUUID());
        rule.setChainTemplate(template);
        rule.setAlertType(alertType);
        rule.setOffsetValue(3);
        rule.setOffsetUnit(DelayUnit.WORKING_DAYS);
        rule.setTargetMode(AlertTargetMode.ROLE);
        rule.setTargetRole(UserRole.EXECUTIVE_OFFICE);
        rule.setActive(true);

        when(filePassageRepository.findActiveCandidatesForAlerts()).thenReturn(List.of(passage));
        when(alertRuleRepository.findByChainTemplateIdAndActiveTrue(template.getId())).thenReturn(List.of(rule));
        when(delaiService.applyOffset(passage.getDueAt(), 3, DelayUnit.WORKING_DAYS)).thenReturn(passage.getDueAt());
        when(responsibleUserResolver.resolve(file, UserRole.EXECUTIVE_OFFICE)).thenReturn(null);

        alertEngineService.evaluateAll();

        verify(notificationService, never()).dispatch(any(Alert.class));
    }

    @Test
    void evaluateAll_skipsRule_whenPriorityScopeUrgentPlusAndFileIsNormal() {
        Instant now = Instant.now();
        AlertRule rule = ruleCurrentResponsible(now, 15);
        rule.setPriorityScope("URGENT_PLUS");
        when(filePassageRepository.findActiveCandidatesForAlerts()).thenReturn(List.of(passage));
        when(alertRuleRepository.findByChainTemplateIdAndActiveTrue(template.getId())).thenReturn(List.of(rule));

        alertEngineService.evaluateAll();

        verify(delaiService, never()).applyOffset(any(), eq(15), any());
        verify(notificationService, never()).dispatch(any(Alert.class));
    }

    @Test
    void evaluateAll_skipsRule_whenScopedToDifferentStep() {
        Instant now = Instant.now();
        ChainStepTemplate otherStep = new ChainStepTemplate();
        otherStep.setId(UUID.randomUUID());
        otherStep.setChainTemplate(template);

        AlertRule rule = ruleCurrentResponsible(now, 0);
        rule.setChainStepTemplate(otherStep);
        when(filePassageRepository.findActiveCandidatesForAlerts()).thenReturn(List.of(passage));
        when(alertRuleRepository.findByChainTemplateIdAndActiveTrue(template.getId())).thenReturn(List.of(rule));

        alertEngineService.evaluateAll();

        verify(notificationService, never()).dispatch(any(Alert.class));
    }
}
