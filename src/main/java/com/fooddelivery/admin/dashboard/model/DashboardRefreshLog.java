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

    // Alias fields for builder compatibility
    @Transient
    private String component;

    @Transient
    private Long durationMs;

    @Transient
    private Boolean success;

    @Transient
    private String message;

    // Post-construct to handle alias fields
    @PostLoad
    @PostPersist
    private void syncFields() {
        if (component != null && panelName == null) {
            panelName = component;
        }
        if (durationMs != null && queryDurationMs == null) {
            queryDurationMs = durationMs.intValue();
        }
        if (success != null && successful == null) {
            successful = success;
        }
        if (message != null && errorMessage == null) {
            errorMessage = message;
        }
    }

    // Builder customization - declare fields that we're setting
    public static class DashboardRefreshLogBuilder {
        private String panelName;
        private Integer queryDurationMs;
        private Boolean successful;
        private String errorMessage;

        public DashboardRefreshLogBuilder component(String component) {
            this.component = component;
            this.panelName = component;
            return this;
        }

        public DashboardRefreshLogBuilder durationMs(Long durationMs) {
            this.durationMs = durationMs;
            this.queryDurationMs = durationMs != null ? durationMs.intValue() : null;
            return this;
        }

        public DashboardRefreshLogBuilder success(Boolean success) {
            this.success = success;
            this.successful = success;
            return this;
        }

        public DashboardRefreshLogBuilder message(String message) {
            this.message = message;
            this.errorMessage = message;
            return this;
        }
    }
}
