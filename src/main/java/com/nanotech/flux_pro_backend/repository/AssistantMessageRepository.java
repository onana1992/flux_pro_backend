package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.AssistantMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AssistantMessageRepository extends JpaRepository<AssistantMessage, UUID> {

    @Query("""
            SELECT m FROM AssistantMessage m
            WHERE m.conversation.id = :conversationId
            ORDER BY m.createdAt ASC
            """)
    List<AssistantMessage> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") UUID conversationId);
}
