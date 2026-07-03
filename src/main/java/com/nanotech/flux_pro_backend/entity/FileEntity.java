package com.nanotech.flux_pro_backend.entity;

import com.nanotech.flux_pro_backend.converter.JsonMapConverter;
import com.nanotech.flux_pro_backend.enumeration.FilePriority;
import com.nanotech.flux_pro_backend.enumeration.FileStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "files")
@Getter
@Setter
public class FileEntity extends BaseEntity {

    @Column(name = "reference_number", length = 32, unique = true)
    private String referenceNumber;

    @Column(name = "file_type_code", nullable = false, length = 32)
    private String fileTypeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chain_template_id")
    private ChainTemplate chainTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(name = "sender_or_beneficiary", nullable = false, length = 255)
    private String senderOrBeneficiary;

    @Column(name = "received_at", nullable = false)
    private LocalDate receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FilePriority priority = FilePriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileStatus status = FileStatus.DRAFT;

    @Column(name = "closure_reason", columnDefinition = "TEXT")
    private String closureReason;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "external_hold_reason", columnDefinition = "TEXT")
    private String externalHoldReason;

    @Column(name = "external_hold_since")
    private Instant externalHoldSince;

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "LONGTEXT")
    private Map<String, Object> metadata;

    @OneToMany(mappedBy = "file")
    private List<FileAttachment> attachments = new ArrayList<>();
}
