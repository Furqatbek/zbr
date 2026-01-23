package com.fooddelivery.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a saved address for a consumer.
 */
@Entity
@Table(name = "consumer_addresses", indexes = {
        @Index(name = "idx_consumer_addresses_user_id", columnList = "user_id"),
        @Index(name = "idx_consumer_addresses_default", columnList = "user_id, is_default")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsumerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 50)
    private String label;

    @Column(name = "full_address", nullable = false, length = 500)
    private String fullAddress;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "apartment_number", length = 50)
    private String apartmentNumber;

    @Column(length = 50)
    private String entrance;

    @Column(length = 500)
    private String instructions;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
