package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubstituteServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubstituteService substituteService;

    private User titular;
    private User substitute;

    @BeforeEach
    void setUp() {
        titular = new User();
        titular.setId(UUID.randomUUID());
        titular.setActive(true);

        substitute = new User();
        substitute.setId(UUID.randomUUID());
        substitute.setActive(true);
    }

    @Test
    void effectiveRecipient_returnsSubstitute_whenActive() {
        titular.setSubstitute(substitute);
        when(userRepository.findByIdWithSubstitute(titular.getId())).thenReturn(Optional.of(titular));

        assertThat(substituteService.effectiveRecipient(titular)).isEqualTo(substitute);
    }

    @Test
    void effectiveRecipient_returnsTitular_whenSubstituteInactive() {
        substitute.setActive(false);
        titular.setSubstitute(substitute);
        when(userRepository.findByIdWithSubstitute(titular.getId())).thenReturn(Optional.of(titular));

        assertThat(substituteService.effectiveRecipient(titular)).isEqualTo(titular);
    }

    @Test
    void wouldCreateCycle_detectsDirectSelf() {
        assertThat(substituteService.wouldCreateCycle(titular.getId(), titular.getId())).isTrue();
    }

    @Test
    void wouldCreateCycle_detectsIndirectLoop() {
        User mid = new User();
        mid.setId(UUID.randomUUID());
        mid.setSubstitute(titular);

        when(userRepository.findByIdWithSubstitute(mid.getId())).thenReturn(Optional.of(mid));

        assertThat(substituteService.wouldCreateCycle(titular.getId(), mid.getId())).isTrue();
    }

    @Test
    void findCoveredUserIds_delegatesToRepository() {
        List<UUID> ids = List.of(titular.getId());
        when(userRepository.findActiveUserIdsBySubstituteId(substitute.getId())).thenReturn(ids);

        assertThat(substituteService.findCoveredUserIds(substitute.getId())).isEqualTo(ids);
    }
}
