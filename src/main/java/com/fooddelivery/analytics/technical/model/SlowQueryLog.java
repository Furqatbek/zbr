package com.fooddelivery.analytics.technical.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity tracking slow database queries for performance analysis.
 */
@Entity
@Table(name = "slow_query_logs", indexes = {
        @Index(name = "idx_sql_timestamp", columnList = "timestamp"),
        @Index(name = "idx_sql_duration", columnList = "duration_ms"),
        @Index(name = "idx_sql_query_hash", columnList = "query_hash")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlowQueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_hash", length = 64)
    private String queryHash;

    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "rows_affected")
    private Long rowsAffected;

    @Column(name = "rows_examined")
    private Long rowsExamined;

    @Column(name = "database_name", length = 100)
    private String databaseName;

    @Column(name = "table_name", length = 100)
    private String tableName;

    @Column(name = "query_type", length = 20)
    @Enumerated(EnumType.STRING)
    private QueryType queryType;

    @Column(name = "is_using_index")
    private Boolean isUsingIndex;

    @Column(name = "index_used", length = 100)
    private String indexUsed;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum QueryType {
        SELECT, INSERT, UPDATE, DELETE, OTHER
    }
}
