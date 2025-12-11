package com.fooddelivery.analytics.technical.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity tracking storage metadata for uploaded files.
 */
@Entity
@Table(name = "storage_metadata", indexes = {
        @Index(name = "idx_sm_storage_type", columnList = "storage_type"),
        @Index(name = "idx_sm_content_type", columnList = "content_type"),
        @Index(name = "idx_sm_uploaded_at", columnList = "uploaded_at"),
        @Index(name = "idx_sm_entity_type", columnList = "entity_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_key", nullable = false, length = 500)
    private String fileKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "storage_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StorageType storageType;

    @Column(name = "bucket_name", length = 100)
    private String bucketName;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "upload_duration_ms")
    private Long uploadDurationMs;

    @Column(name = "is_upload_successful")
    @Builder.Default
    private Boolean isUploadSuccessful = true;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum StorageType {
        LOCAL, S3, MINIO, GCS, AZURE_BLOB
    }

    /**
     * Check if file is an image.
     */
    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Get file size in MB.
     */
    public double getFileSizeMb() {
        return fileSizeBytes != null ? fileSizeBytes / (1024.0 * 1024.0) : 0.0;
    }
}
