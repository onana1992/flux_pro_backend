package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.AssistantConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AssistantConversationRepository extends JpaRepository<AssistantConversation, UUID> {

    @Query("""
            SELECT c FROM AssistantConversation c
            WHERE c.id = :id AND c.user.id = :userId
            """)
    Optional<AssistantConversation> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
}
