package com.nanotech.flux_pro_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "assistant_tool_call")
@Getter
@Setter
public class AssistantToolCall extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private AssistantMessage message;

    @Column(name = "tool_name", nullable = false, length = 80)
    private String toolName;

    @Column(name = "arguments_json", columnDefinition = "TEXT")
    private String argumentsJson;

    @Column(name = "result_summary", length = 500)
    private String resultSummary;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "duration_ms")
    private Integer durationMs;
}
