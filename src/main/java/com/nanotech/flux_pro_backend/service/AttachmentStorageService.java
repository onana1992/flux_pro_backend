package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.Organization;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Stockage des pièces jointes (disque local ou AWS S3).
 * Métadonnées en base : {@code storage_bucket} + {@code storage_key}.
 */
public interface AttachmentStorageService {

    /**
     * Persiste le contenu et retourne la clé objet
     * ({@code orgCode/year/fileId/uuid_filename}).
     */
    String store(
            Organization organization,
            UUID fileId,
            String originalFilename,
            InputStream content,
            long contentLength,
            String contentType) throws IOException;

    Resource loadAsResource(String storageBucket, String storageKey) throws IOException;

    void delete(String storageBucket, String storageKey) throws IOException;

    /** Valeur à enregistrer dans {@code file_attachments.storage_bucket}. */
    String defaultBucket();

    String providerId();
}
