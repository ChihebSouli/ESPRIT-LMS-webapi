package com.esprit.lms.shared.storage;

import com.esprit.lms.shared.exception.ApiException;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO storage service — handles all file operations.
 *
 * Two buckets:
 *   - lms-documents (PRIVATE) — PDFs, eBooks, PFE reports
 *   - lms-covers    (PUBLIC)  — cover images
 *
 * Object keys use UUID format: documents/{uuid}/{uuid}.ext
 * NEVER use original filenames to prevent path traversal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.documents}")
    private String documentsBucket;

    @Value("${minio.bucket.covers}")
    private String coversBucket;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * Upload a document to private bucket.
     * Returns the MinIO object key (NEVER expose this to frontend).
     */
    public String uploadDocument(MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename());
        String objectKey = "documents/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ext;

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(documentsBucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("Uploaded document: {} ({} bytes)", objectKey, file.getSize());
            return objectKey;
        } catch (Exception e) {
            log.error("Document upload failed", e);
            throw ApiException.serviceUnavailable("File storage unavailable: " + e.getMessage());
        }
    }

    /**
     * Upload a cover image to public bucket.
     * Returns the public URL.
     */
    public String uploadCover(MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename());
        String objectKey = "covers/" + UUID.randomUUID() + ext;

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(coversBucket)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            String publicUrl = endpoint + "/" + coversBucket + "/" + objectKey;
            log.info("Uploaded cover: {}", publicUrl);
            return publicUrl;
        } catch (Exception e) {
            log.error("Cover upload failed", e);
            throw ApiException.serviceUnavailable("File storage unavailable: " + e.getMessage());
        }
    }

    /**
     * Generate a presigned URL for reading a private document.
     * 1-hour expiry — this is the DRM mechanism.
     */
    public String generatePresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(documentsBucket)
                    .object(objectKey)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            log.error("Presigned URL generation failed for {}", objectKey, e);
            throw ApiException.serviceUnavailable("Unable to generate read access");
        }
    }

    /**
     * Get document as InputStream (for watermarking, text extraction).
     */
    public InputStream getDocumentStream(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(documentsBucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("Document fetch failed for {}", objectKey, e);
            throw ApiException.serviceUnavailable("File storage unavailable");
        }
    }

    /**
     * Delete a document from private bucket.
     */
    public void deleteDocument(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(documentsBucket)
                    .object(objectKey)
                    .build());
            log.info("Deleted document: {}", objectKey);
        } catch (Exception e) {
            log.error("Document delete failed for {}", objectKey, e);
            throw ApiException.serviceUnavailable("File storage unavailable");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
