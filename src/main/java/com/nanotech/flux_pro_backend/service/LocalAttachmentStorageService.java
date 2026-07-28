package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.Organization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Year;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "fluxpro.attachments.storage", havingValue = "local", matchIfMissing = true)
public class LocalAttachmentStorageService implements AttachmentStorageService {

    public static final String BUCKET = "local";

    private final Path rootPath;

    public LocalAttachmentStorageService(
            @Value("${fluxpro.attachments.storage-path:./data/attachments}") String storagePath) {
        this.rootPath = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @Override
    public String store(
            Organization organization,
            UUID fileId,
            String originalFilename,
            InputStream content,
            long contentLength,
            String contentType) throws IOException {
        String orgCode = organization.getCode();
        int year = Year.now().getValue();
        String safeName = sanitizeFilename(originalFilename);
        String key = orgCode + "/" + year + "/" + fileId + "/" + UUID.randomUUID() + "_" + safeName;
        Path target = rootPath.resolve(key).normalize();
        if (!target.startsWith(rootPath)) {
            throw new IOException("Invalid storage path");
        }
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    @Override
    public Resource loadAsResource(String storageBucket, String storageKey) throws IOException {
        Path file = rootPath.resolve(storageKey).normalize();
        if (!file.startsWith(rootPath) || !Files.exists(file)) {
            throw new IOException("Attachment not found: " + storageKey);
        }
        return new UrlResource(file.toUri());
    }

    @Override
    public void delete(String storageBucket, String storageKey) throws IOException {
        Path file = rootPath.resolve(storageKey).normalize();
        if (file.startsWith(rootPath) && Files.exists(file)) {
            Files.delete(file);
        }
    }

    @Override
    public String defaultBucket() {
        return BUCKET;
    }

    @Override
    public String providerId() {
        return "local";
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
