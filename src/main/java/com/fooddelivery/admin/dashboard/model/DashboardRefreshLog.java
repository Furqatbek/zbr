package com.fooddelivery.admin.dashboard.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for tracking dashboard refresh events and performance.
 */
@Entity
@Table(name = "dashboard_refresh_logs", indexes = {
        @Index(name = "idx_refresh_panel", columnList = "panel_name"),
        @Index(name = "idx_refresh_time", columnList = "refreshed_at"),
        @Index(name = "idx_refresh_user", columnList = "admin_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardRefreshLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "panel_name", nullable = false, length = 50)
    private String panelName; // OVERVIEW, ORDERS, RESTAURANTS, COURIERS, FINANCE, SUPPORT

    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(name = "query_duration_ms")
    private Integer queryDurationMs;

    @Column(name = "cache_hit")
    @Builder.Default
    private Boolean cacheHit = false;

    @Column(name = "records_returned")
    private Integer recordsReturned;

    @Column(name = "filter_params", length = 1000)
    private String filterParams; // JSON representation of applied filters

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "successful")
    @Builder.Default
    private Boolean successful = true;

    @CreationTimestamp
    @Column(name = "refreshed_at", nullable = false, updatable = false)
    private LocalDateTime refreshedAt;
}
