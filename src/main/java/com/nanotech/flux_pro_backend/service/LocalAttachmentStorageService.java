package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.Organization;
import org.springframework.beans.factory.annotation.Value;
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
public class LocalAttachmentStorageService {

    static final String BUCKET = "local";

    private final Path rootPath;

    public LocalAttachmentStorageService(
            @Value("${fluxpro.attachments.storage-path:./data/attachments}") String storagePath) {
        this.rootPath = Path.of(storagePath).toAbsolutePath().normalize();
    }

    public String store(
            Organization organization,
            UUID fileId,
            String originalFilename,
            InputStream content) throws IOException {
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

    public Resource loadAsResource(String storageKey) throws IOException {
        Path file = rootPath.resolve(storageKey).normalize();
        if (!file.startsWith(rootPath) || !Files.exists(file)) {
            throw new IOException("Attachment not found: " + storageKey);
        }
        return new UrlResource(file.toUri());
    }

    public void delete(String storageKey) throws IOException {
        Path file = rootPath.resolve(storageKey).normalize();
        if (file.startsWith(rootPath) && Files.exists(file)) {
            Files.delete(file);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
