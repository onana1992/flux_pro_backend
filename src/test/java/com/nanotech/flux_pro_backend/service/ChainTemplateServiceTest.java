package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.ChainTemplateException;
import com.nanotech.flux_pro_backend.dto.request.ChainStepTemplateRequest;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.ChainTemplateRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.PreconfiguredDossierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChainTemplateServiceTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private ChainTemplateRepository chainTemplateRepository;

    @Mock
    private ChainTemplateUsageService chainTemplateUsageService;

    @Mock
    private PreconfiguredDossierRepository preconfiguredDossierRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private ChainTemplateService chainTemplateService;

    private ChainTemplate systemTemplate;

    @BeforeEach
    void setUp() {
        systemTemplate = new ChainTemplate();
        systemTemplate.setId(UUID.randomUUID());
        systemTemplate.setCode("T01");
        systemTemplate.setSystemTemplate(true);
        systemTemplate.setTotalDelayDays(11);
        systemTemplate.setDelayUnit(DelayUnit.WORKING_DAYS);

        Organization org = new Organization();
        org.setId(ORG_ID);
        lenient().when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        lenient().when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
    }

    @Test
    void validateSteps_rejectsOrderGap() {
        List<ChainStepTemplateRequest> steps = List.of(
                step(1, false),
                step(3, true));

        assertThatThrownBy(() -> chainTemplateService.validateSteps(systemTemplate, steps))
                .isInstanceOf(ChainTemplateException.class)
                .hasMessageContaining("consecutive");
    }

    @Test
    void validateSteps_rejectsInvalidClosureCount() {
        List<ChainStepTemplateRequest> steps = List.of(
                step(1, false),
                step(2, false));

        assertThatThrownBy(() -> chainTemplateService.validateSteps(systemTemplate, steps))
                .isInstanceOf(ChainTemplateException.class)
                .hasMessageContaining("closure");
    }

    @Test
    void validateSteps_rejectsDelaySumExceeded() {
        systemTemplate.setTotalDelayDays(2);
        List<ChainStepTemplateRequest> steps = List.of(
                step(1, "A", UserRole.AGENT, 3, false),
                step(2, "B", UserRole.AGENT, 0, true));

        assertThatThrownBy(() -> chainTemplateService.validateSteps(systemTemplate, steps))
                .isInstanceOf(ChainTemplateException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void delete_rejectsSystemTemplate() {
        UUID id = systemTemplate.getId();
        when(chainTemplateRepository.findByIdWithSteps(id)).thenReturn(Optional.of(systemTemplate));

        assertThatThrownBy(() -> chainTemplateService.delete(id))
                .isInstanceOf(ChainTemplateException.class)
                .hasMessageContaining("System chain templates");

        verify(chainTemplateRepository, never()).delete(any());
    }

    @Test
    void deactivate_rejectsWhenInUse() {
        UUID id = systemTemplate.getId();
        when(chainTemplateRepository.findByIdWithSteps(id)).thenReturn(Optional.of(systemTemplate));
        when(chainTemplateUsageService.hasInProgressFiles(id)).thenReturn(true);

        assertThatThrownBy(() -> chainTemplateService.deactivate(id))
                .isInstanceOf(ChainTemplateException.class)
                .hasMessageContaining("in-progress");
    }

    @Test
    void replaceSteps_replacesAllSteps() {
        UUID id = systemTemplate.getId();
        systemTemplate.setSystemTemplate(false);
        when(chainTemplateRepository.findByIdWithSteps(id)).thenReturn(Optional.of(systemTemplate));
        when(chainTemplateRepository.save(systemTemplate)).thenReturn(systemTemplate);

        List<ChainStepTemplateRequest> steps = List.of(
                step(1, "A", UserRole.AGENT, 1, false),
                step(2, "B", UserRole.AGENT, 0, true));

        chainTemplateService.replaceSteps(id, steps);

        verify(chainTemplateRepository).save(systemTemplate);
        assert systemTemplate.getSteps().size() == 2;
    }

    @Test
    void delete_rejectsWhenLinkedToFiles() {
        UUID id = UUID.randomUUID();
        ChainTemplate custom = new ChainTemplate();
        custom.setId(id);
        custom.setSystemTemplate(false);
        when(chainTemplateRepository.findByIdWithSteps(id)).thenReturn(Optional.of(custom));
        when(chainTemplateUsageService.isAssociatedWithFiles(id)).thenReturn(true);

        assertThatThrownBy(() -> chainTemplateService.delete(id))
                .isInstanceOf(ChainTemplateException.class)
                .hasMessageContaining("already linked to files");

        verify(chainTemplateRepository, never()).delete(any());
    }

    @Test
    void validateSteps_acceptsParallelStepsInSameStage() {
        List<ChainStepTemplateRequest> steps = List.of(
                step(1, "Visa A", UserRole.AGENT, 2, false),
                step(1, "Visa B", UserRole.DIRECTOR, 3, false),
                step(2, "Clôture", UserRole.AGENT, 0, true));

        chainTemplateService.validateSteps(systemTemplate, steps);
    }

    @Test
    void validateSteps_rejectsParallelClosureStage() {
        List<ChainStepTemplateRequest> steps = List.of(
                step(1, "A", UserRole.AGENT, 1, false),
                step(2, "Clôture A", UserRole.AGENT, 0, true),
                step(2, "Clôture B", UserRole.AGENT, 0, false));

        assertThatThrownBy(() -> chainTemplateService.validateSteps(systemTemplate, steps))
                .isInstanceOf(ChainTemplateException.class)
                .hasMessageContaining("only step");
    }

    private ChainStepTemplateRequest step(int order, boolean closure) {
        return step(order, "Step " + order, UserRole.AGENT, closure ? 0 : 1, closure);
    }

    private ChainStepTemplateRequest step(
            int order, String label, UserRole role, int delayValue, boolean closure) {
        return new ChainStepTemplateRequest(
                null,
                order,
                label,
                role,
                ORG_ID,
                delayValue,
                DelayUnit.WORKING_DAYS,
                null,
                false,
                closure);
    }
}
