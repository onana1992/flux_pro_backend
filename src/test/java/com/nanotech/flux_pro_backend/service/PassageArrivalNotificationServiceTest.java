package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.Alert;
import com.nanotech.flux_pro_backend.entity.AlertType;
import com.nanotech.flux_pro_backend.entity.ChainStepTemplate;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.enumeration.AlertChannel;
import com.nanotech.flux_pro_backend.repository.AlertRepository;
import com.nanotech.flux_pro_backend.repository.AlertTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassageArrivalNotificationServiceTest {

    @Mock
    private AlertTypeRepository alertTypeRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SubstituteService substituteService;

    @InjectMocks
    private PassageArrivalNotificationService service;

    private FilePassage passage;
    private User responsible;
    private User ccUser;
    private AlertType arrivalType;
    private AlertType ccType;

    @BeforeEach
    void setUp() {
        FileEntity file = new FileEntity();
        file.setId(UUID.randomUUID());
        file.setReferenceNumber("REF-1");

        ChainStepTemplate step = new ChainStepTemplate();
        step.setId(UUID.randomUUID());
        step.setLabel("Instruction");

        responsible = new User();
        responsible.setId(UUID.randomUUID());
        responsible.setActive(true);

        ccUser = new User();
        ccUser.setId(UUID.randomUUID());
        ccUser.setActive(true);

        passage = new FilePassage();
        passage.setId(UUID.randomUUID());
        passage.setFile(file);
        passage.setChainStepTemplate(step);
        passage.setResponsibleUser(responsible);
        passage.setCcUsers(List.of(ccUser));
        passage.setReceivedAt(Instant.now());

        arrivalType = new AlertType();
        arrivalType.setId(UUID.randomUUID());
        arrivalType.setCode(PassageArrivalNotificationService.TYPE_ARRIVAL);
        arrivalType.setActive(true);

        ccType = new AlertType();
        ccType.setId(UUID.randomUUID());
        ccType.setCode(PassageArrivalNotificationService.TYPE_CC);
        ccType.setActive(true);

        when(notificationService.activeChannels()).thenReturn(List.of(AlertChannel.IN_APP, AlertChannel.EMAIL));
        org.mockito.Mockito.lenient().when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));
        when(substituteService.effectiveRecipient(responsible)).thenReturn(responsible);
        when(alertTypeRepository.findByCodeIgnoreCase(PassageArrivalNotificationService.TYPE_ARRIVAL))
                .thenReturn(Optional.of(arrivalType));
        when(alertTypeRepository.findByCodeIgnoreCase(PassageArrivalNotificationService.TYPE_CC))
                .thenReturn(Optional.of(ccType));
        org.mockito.Mockito.lenient()
                .when(alertRepository.existsArrivalNotification(any(), any(), any(), any(), any()))
                .thenReturn(false);
    }

    @Test
    void notifyArrival_sendsToResponsibleAndCc() {
        service.notifyArrival(passage);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(notificationService, atLeastOnce()).dispatch(captor.capture());
        List<Alert> alerts = captor.getAllValues();
        assertThat(alerts).hasSize(4); // 2 recipients × 2 channels
        assertThat(alerts).anyMatch(a -> a.getRecipient().getId().equals(responsible.getId())
                && a.getAlertType().getCode().equals(PassageArrivalNotificationService.TYPE_ARRIVAL));
        assertThat(alerts).anyMatch(a -> a.getRecipient().getId().equals(ccUser.getId())
                && a.getAlertType().getCode().equals(PassageArrivalNotificationService.TYPE_CC));
    }

    @Test
    void notifyArrival_skipsWhenAlreadyNotifiedForThisActivation() {
        when(alertRepository.existsArrivalNotification(any(), any(), any(), any(), any())).thenReturn(true);

        service.notifyArrival(passage);

        verify(notificationService, never()).dispatch(any());
    }
}
