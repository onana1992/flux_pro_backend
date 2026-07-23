package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.User;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Résolution d'intérim (ORG-04) : un {@code substitute_id} actif délègue
 * l'action et les alertes au suppléant, sans changer le responsable du maillon.
 */
@Service
@RequiredArgsConstructor
public class SubstituteService {

    private static final int MAX_CHAIN_DEPTH = 20;

    private final UserRepository userRepository;

    /**
     * Destinataire effectif des alertes : le suppléant actif s'il existe, sinon le titulaire.
     */
    @Transactional(readOnly = true)
    public User effectiveRecipient(User user) {
        if (user == null) {
            return null;
        }
        return userRepository.findByIdWithSubstitute(user.getId())
                .map(loaded -> {
                    User substitute = loaded.getSubstitute();
                    if (substitute != null && substitute.isActive()) {
                        return substitute;
                    }
                    return loaded;
                })
                .orElse(user);
    }

    /** True si {@code actorId} est le suppléant actif du titulaire. */
    @Transactional(readOnly = true)
    public boolean isActiveSubstituteOf(UUID actorId, User titular) {
        if (actorId == null || titular == null || titular.getId() == null) {
            return false;
        }
        return userRepository.isActiveSubstitute(titular.getId(), actorId);
    }

    /** Identifiants des utilisateurs actifs dont le suppléant est {@code substituteId}. */
    @Transactional(readOnly = true)
    public List<UUID> findCoveredUserIds(UUID substituteId) {
        if (substituteId == null) {
            return List.of();
        }
        return userRepository.findActiveUserIdsBySubstituteId(substituteId);
    }

    /**
     * Détecte un cycle dans la chaîne de suppléance si {@code userId} désignait
     * {@code substituteId} (ex. A→B alors que B…→A).
     */
    @Transactional(readOnly = true)
    public boolean wouldCreateCycle(UUID userId, UUID substituteId) {
        if (userId == null || substituteId == null) {
            return false;
        }
        if (userId.equals(substituteId)) {
            return true;
        }
        UUID cursor = substituteId;
        for (int depth = 0; depth < MAX_CHAIN_DEPTH && cursor != null; depth++) {
            if (cursor.equals(userId)) {
                return true;
            }
            cursor = userRepository.findByIdWithSubstitute(cursor)
                    .map(User::getSubstitute)
                    .map(User::getId)
                    .orElse(null);
        }
        return false;
    }
}
