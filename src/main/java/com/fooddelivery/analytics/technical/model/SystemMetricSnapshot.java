package com.fooddelivery.analytics.technical.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity storing periodic snapshots of system metrics.
 */
@Entity
@Table(name = "system_metric_snapshots", indexes = {
        @Index(name = "idx_sms_metric_type", columnList = "metric_type"),
        @Index(name = "idx_sms_recorded_at", columnList = "recorded_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemMetricSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private MetricType metricType;

    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    @Column(name = "metric_value", nullable = false)
    private Double metricValue;

    @Column(name = "metric_unit", length = 20)
    private String metricUnit;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "host", length = 100)
    private String host;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum MetricType {
        CPU, MEMORY, DISK, NETWORK, JVM, DATABASE, CACHE, QUEUE
    }
}
