package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.AssistantToolCall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssistantToolCallRepository extends JpaRepository<AssistantToolCall, UUID> {
}
