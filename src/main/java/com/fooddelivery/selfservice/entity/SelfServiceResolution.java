package com.fooddelivery.selfservice.entity;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Record of self-service resolutions used by customers.
 */
@Entity
@Table(name = "self_service_resolutions", indexes = {
        @Index(name = "idx_ssr_user_id", columnList = "user_id"),
        @Index(name = "idx_ssr_order_id", columnList = "order_id"),
        @Index(name = "idx_ssr_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelfServiceResolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type", nullable = false, length = 50)
    private SelfServiceResolutionType resolutionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resolution_details", columnDefinition = "jsonb")
    private Map<String, Object> resolutionDetails;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "COMPLETED";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
