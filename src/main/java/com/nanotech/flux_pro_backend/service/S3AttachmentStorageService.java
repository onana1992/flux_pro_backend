package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.Organization;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.time.Year;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "fluxpro.attachments.storage", havingValue = "s3")
public class S3AttachmentStorageService implements AttachmentStorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String region;

    public S3AttachmentStorageService(
            @Value("${fluxpro.attachments.s3.bucket}") String bucket,
            @Value("${fluxpro.attachments.s3.region:us-east-1}") String region,
            @Value("${fluxpro.attachments.s3.access-key-id:}") String accessKeyId,
            @Value("${fluxpro.attachments.s3.secret-access-key:}") String secretAccessKey) {
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException(
                    "fluxpro.attachments.s3.bucket is required when storage=s3");
        }
        this.bucket = bucket.trim();
        this.region = region.trim();
        this.s3Client = S3Client.builder()
                .region(Region.of(this.region))
                .credentialsProvider(credentialsProvider(accessKeyId, secretAccessKey))
                .build();
    }

    private static AwsCredentialsProvider credentialsProvider(String accessKeyId, String secretAccessKey) {
        if (StringUtils.hasText(accessKeyId) && StringUtils.hasText(secretAccessKey)) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId.trim(), secretAccessKey.trim()));
        }
        return DefaultCredentialsProvider.create();
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

        String resolvedContentType =
                StringUtils.hasText(contentType) ? contentType : "application/octet-stream";

        try {
            PutObjectRequest.Builder put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(resolvedContentType);
            if (contentLength >= 0) {
                put.contentLength(contentLength);
            }
            s3Client.putObject(
                    put.build(),
                    contentLength >= 0
                            ? RequestBody.fromInputStream(content, contentLength)
                            : RequestBody.fromBytes(content.readAllBytes()));
            return key;
        } catch (S3Exception e) {
            throw new IOException("S3 putObject failed: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public Resource loadAsResource(String storageBucket, String storageKey) throws IOException {
        String resolvedBucket = resolveBucket(storageBucket);
        try {
            var response = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(resolvedBucket)
                    .key(storageKey)
                    .build());
            return new InputStreamResource(response) {
                @Override
                public String getFilename() {
                    int slash = storageKey.lastIndexOf('/');
                    return slash >= 0 ? storageKey.substring(slash + 1) : storageKey;
                }

                @Override
                public long contentLength() {
                    long len = response.response().contentLength();
                    return len >= 0 ? len : -1;
                }
            };
        } catch (NoSuchKeyException e) {
            throw new IOException("Attachment not found in S3: " + storageKey, e);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new IOException("Attachment not found in S3: " + storageKey, e);
            }
            throw new IOException("S3 getObject failed: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public void delete(String storageBucket, String storageKey) throws IOException {
        String resolvedBucket = resolveBucket(storageBucket);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(resolvedBucket)
                    .key(storageKey)
                    .build());
        } catch (S3Exception e) {
            throw new IOException("S3 deleteObject failed: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public String defaultBucket() {
        return bucket;
    }

    @Override
    public String providerId() {
        return "s3";
    }

    private String resolveBucket(String storageBucket) {
        if (StringUtils.hasText(storageBucket) && !"local".equalsIgnoreCase(storageBucket)) {
            return storageBucket.trim();
        }
        return bucket;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    @PreDestroy
    void close() {
        s3Client.close();
    }
}
