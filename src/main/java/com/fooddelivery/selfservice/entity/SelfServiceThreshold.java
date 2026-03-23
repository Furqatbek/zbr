package com.fooddelivery.selfservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuration for self-service resolution thresholds.
 * Controls automatic resolution limits and requirements.
 */
@Entity
@Table(name = "self_service_thresholds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelfServiceThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type", nullable = false, unique = true, length = 50)
    private SelfServiceResolutionType resolutionType;

    @Column(name = "max_auto_refund", precision = 10, scale = 2)
    private BigDecimal maxAutoRefund;

    @Column(name = "max_daily_resolutions")
    @Builder.Default
    private Integer maxDailyResolutions = 3;

    @Column(name = "require_photo")
    @Builder.Default
    private Boolean requirePhoto = false;

    @Column
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
