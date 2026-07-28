package com.nanotech.flux_pro_backend.entity;

import com.nanotech.flux_pro_backend.enumeration.AttachmentKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_attachments")
@Getter
@Setter
public class FileAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_bucket", nullable = false, length = 63)
    private String storageBucket;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    /**
     * @deprecated remplacé par {@link #attachmentKind} == {@link AttachmentKind#CLOSURE} ;
     * conservé pour compatibilité / backfill.
     */
    @Column(name = "response_document", nullable = false)
    private boolean responseDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_kind", nullable = false, length = 20)
    private AttachmentKind attachmentKind = AttachmentKind.CREATION;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passage_id")
    private FilePassage passage;

    /** Visible sur le portail (surtout CLOSURE pour dossiers portail). */
    @Column(name = "portal_visible", nullable = false)
    private boolean portalVisible;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_portal_user_id")
    private PortalUser uploadedByPortalUser;

    @Column(name = "portal_attachment_key", length = 64)
    private String portalAttachmentKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (attachmentKind == null) {
            attachmentKind = responseDocument ? AttachmentKind.CLOSURE : AttachmentKind.CREATION;
        }
        responseDocument = attachmentKind == AttachmentKind.CLOSURE;
    }
}
