package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.dto.request.ClockAdjustRequest;
import com.nanotech.flux_pro_backend.dto.response.SystemClockResponse;
import com.nanotech.flux_pro_backend.entity.SystemClockState;
import com.nanotech.flux_pro_backend.enumeration.ClockAdjustUnit;
import com.nanotech.flux_pro_backend.enumeration.ClockMode;
import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.SystemClockRepository;
import com.nanotech.flux_pro_backend.security.SecurityUser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Horloge système unique. En mode NORMAL, délègue à {@link Instant#now()}.
 * En mode TEST, lit/écrit l'ancre en base : le temps effectif tick avec le mur
 * à partir de {@code artificialNow} synchronisé à {@code wallSyncedAt}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClockService {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Douala");

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(BUSINESS_ZONE);

    private final SystemClockRepository systemClockRepository;
    private final DelaiService delaiService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${fluxpro.clock.mode:normal}")
    private String modeProperty;

    private ClockMode mode = ClockMode.NORMAL;

    @PostConstruct
    void init() {
        mode = parseMode(modeProperty);
        if (mode == ClockMode.TEST) {
            try {
                ensureState();
                log.warn("Clock mode TEST actif — now effectif = {}", now());
            } catch (Exception e) {
                log.error(
                        "Mode TEST : table system_clock inaccessible. Exécutez docs/sql/2026-07-14_system_clock.sql — {}",
                        e.getMessage());
            }
        } else {
            log.info("Clock mode NORMAL — Instant.now() réel");
        }
    }

    public ClockMode getMode() {
        return mode;
    }

    public boolean isTestMode() {
        return mode == ClockMode.TEST;
    }

    /** Instant courant consommé par les services métier (alertes, passages, dashboard…). */
    public Instant now() {
        if (mode != ClockMode.TEST) {
            return Instant.now();
        }
        SystemClockState state = findStateOrNull();
        if (state == null) {
            return Instant.now();
        }
        Instant wall = Instant.now();
        Duration elapsed = Duration.between(state.getWallSyncedAt(), wall);
        if (elapsed.isNegative()) {
            elapsed = Duration.ZERO;
        }
        return state.getArtificialNow().plus(elapsed);
    }

    @Transactional(readOnly = true)
    public SystemClockResponse status() {
        Instant current = now();
        if (mode != ClockMode.TEST) {
            return new SystemClockResponse(
                    ClockMode.NORMAL, current, BUSINESS_ZONE.getId(), DISPLAY.format(current), false, null, null);
        }
        SystemClockState state = findStateOrNull();
        return new SystemClockResponse(
                ClockMode.TEST,
                current,
                BUSINESS_ZONE.getId(),
                DISPLAY.format(current),
                true,
                state != null ? state.getArtificialNow() : null,
                state != null ? state.getWallSyncedAt() : null);
    }

    @Transactional
    public SystemClockResponse adjust(ClockAdjustRequest request, SecurityUser actor) {
        assertMutable(actor);
        Instant previous = now();
        Instant target = applyAdjust(previous, request.amount(), request.unit());
        pin(target);
        publishMoved(previous, target);
        log.info("Clock TEST ajustée de {} {} → {}", request.amount(), request.unit(), target);
        return status();
    }

    @Transactional
    public SystemClockResponse set(Instant instant, SecurityUser actor) {
        assertMutable(actor);
        Instant previous = now();
        pin(instant);
        publishMoved(previous, instant);
        log.info("Clock TEST positionnée sur {}", instant);
        return status();
    }

    @Transactional
    public SystemClockResponse resetToWall(SecurityUser actor) {
        assertMutable(actor);
        Instant previous = now();
        Instant wall = Instant.now();
        pin(wall);
        publishMoved(previous, wall);
        log.info("Clock TEST réinitialisée sur le mur {}", wall);
        return status();
    }

    /** Vérifie mode TEST + rôle admin (ex. déclenchement manuel du moteur d'alertes). */
    public void assertCanMutate(SecurityUser actor) {
        assertMutable(actor);
    }

    private Instant applyAdjust(Instant from, int amount, ClockAdjustUnit unit) {
        return switch (unit) {
            case HOURS -> from.plus(amount, ChronoUnit.HOURS);
            case DAYS -> from.plus(amount, ChronoUnit.DAYS);
            case WORKING_DAYS -> delaiService.applyOffset(from, amount, DelayUnit.WORKING_DAYS);
            case WORKING_HOURS -> delaiService.applyOffset(from, amount, DelayUnit.WORKING_HOURS);
        };
    }

    private void pin(Instant artificialNow) {
        Instant wall = Instant.now();
        SystemClockState state = ensureState();
        state.setArtificialNow(artificialNow);
        state.setWallSyncedAt(wall);
        systemClockRepository.save(state);
    }

    private void publishMoved(Instant previous, Instant next) {
        eventPublisher.publishEvent(new SystemClockMovedEvent(previous, next));
    }

    private SystemClockState ensureState() {
        return systemClockRepository.findAll().stream().findFirst().orElseGet(() -> {
            Instant wall = Instant.now();
            SystemClockState created = new SystemClockState();
            created.setArtificialNow(wall);
            created.setWallSyncedAt(wall);
            return systemClockRepository.save(created);
        });
    }

    private SystemClockState findStateOrNull() {
        return systemClockRepository.findAll().stream().findFirst().orElse(null);
    }

    private void assertMutable(SecurityUser actor) {
        if (mode != ClockMode.TEST) {
            throw AppException.badRequest(
                    "CLOCK_NOT_IN_TEST_MODE",
                    "Clock can only be adjusted when fluxpro.clock.mode=test");
        }
        UserRole role = actor.getRole();
        if (role != UserRole.SUPER_ADMIN && role != UserRole.BUSINESS_ADMIN) {
            throw AppException.forbidden(
                    "CLOCK_ADJUST_FORBIDDEN",
                    "Only SUPER_ADMIN or BUSINESS_ADMIN can adjust the test clock");
        }
    }

    private static ClockMode parseMode(String value) {
        if (value == null || value.isBlank()) {
            return ClockMode.NORMAL;
        }
        return switch (value.trim().toLowerCase()) {
            case "test" -> ClockMode.TEST;
            default -> ClockMode.NORMAL;
        };
    }

    /** Affichage local Douala (hors API) — utile pour logs. */
    public ZonedDateTime nowZoned() {
        return ZonedDateTime.ofInstant(now(), BUSINESS_ZONE);
    }
}
