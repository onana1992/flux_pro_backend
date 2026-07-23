package com.nanotech.flux_pro_backend.entity;

import com.nanotech.flux_pro_backend.enumeration.PassageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "file_passages")
@Getter
@Setter
public class FilePassage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chain_step_template_id", nullable = false)
    private ChainStepTemplate chainStepTemplate;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private User responsibleUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PassageStatus status = PassageStatus.PENDING;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "transmitted_at")
    private Instant transmittedAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "consumed_hours", precision = 10, scale = 2)
    private BigDecimal consumedHours;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "internal_comment", columnDefinition = "TEXT")
    private String internalComment;

    @Column(name = "return_reason", length = 500)
    private String returnReason;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "resumed_at")
    private Instant resumedAt;

    /** Copies informées (CHN-09) — notifiées à l'arrivée, sans droit de transmission. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "file_passage_cc",
            joinColumns = @JoinColumn(name = "file_passage_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> ccUsers = new ArrayList<>();
}
