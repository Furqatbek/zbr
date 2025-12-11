package com.fooddelivery.analytics.technical.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity tracking database backup operations.
 */
@Entity
@Table(name = "backup_logs", indexes = {
        @Index(name = "idx_bl_backup_type", columnList = "backup_type"),
        @Index(name = "idx_bl_status", columnList = "status"),
        @Index(name = "idx_bl_started_at", columnList = "started_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "backup_id", nullable = false, length = 50)
    private String backupId;

    @Column(name = "backup_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BackupType backupType;

    @Column(name = "database_name", length = 100)
    private String databaseName;

    @Column(name = "backup_size_bytes")
    private Long backupSizeBytes;

    @Column(name = "backup_location", length = 500)
    private String backupLocation;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BackupStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "tables_backed_up")
    private Integer tablesBackedUp;

    @Column(name = "rows_backed_up")
    private Long rowsBackedUp;

    @Column(name = "is_compressed")
    @Builder.Default
    private Boolean isCompressed = true;

    @Column(name = "is_encrypted")
    @Builder.Default
    private Boolean isEncrypted = false;

    @Column(name = "retention_days")
    private Integer retentionDays;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum BackupType {
        FULL, INCREMENTAL, DIFFERENTIAL, TRANSACTION_LOG
    }

    public enum BackupStatus {
        IN_PROGRESS, COMPLETED, FAILED, CANCELLED, VERIFYING
    }

    /**
     * Check if backup was successful.
     */
    public boolean isSuccessful() {
        return status == BackupStatus.COMPLETED;
    }

    /**
     * Get backup size in MB.
     */
    public double getBackupSizeMb() {
        return backupSizeBytes != null ? backupSizeBytes / (1024.0 * 1024.0) : 0.0;
    }
}
