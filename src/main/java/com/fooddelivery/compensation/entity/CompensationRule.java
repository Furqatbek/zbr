package com.fooddelivery.compensation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity defining automatic compensation rules.
 */
@Entity
@Table(name = "compensation_rules", indexes = {
        @Index(name = "idx_cr_trigger_type", columnList = "trigger_type"),
        @Index(name = "idx_cr_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompensationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 50)
    private CompensationTriggerType triggerType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "condition_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> conditionJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_type", nullable = false, length = 30)
    private CompensationType compensationType;

    @Column(name = "compensation_value", precision = 10, scale = 2)
    private BigDecimal compensationValue;

    @Column(name = "compensation_percent", precision = 5, scale = 2)
    private BigDecimal compensationPercent;

    @Column(name = "max_compensation", precision = 10, scale = 2)
    private BigDecimal maxCompensation;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
