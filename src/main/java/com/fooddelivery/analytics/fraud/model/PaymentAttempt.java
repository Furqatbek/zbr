package com.fooddelivery.analytics.fraud.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing payment attempts for fraud detection.
 */
@Entity
@Table(name = "payment_attempts", indexes = {
        @Index(name = "idx_payment_attempts_user_id", columnList = "user_id"),
        @Index(name = "idx_payment_attempts_order_id", columnList = "order_id"),
        @Index(name = "idx_payment_attempts_status", columnList = "status"),
        @Index(name = "idx_payment_attempts_created_at", columnList = "created_at"),
        @Index(name = "idx_payment_attempts_user_status_created", columnList = "user_id, status, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "card_bin", length = 6)
    private String cardBin;

    @Column(name = "payment_token_hash", length = 64)
    private String paymentTokenHash;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "gateway_response", length = 500)
    private String gatewayResponse;

    @Column(name = "is_3ds_verified")
    private Boolean is3dsVerified;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED, DECLINED, TIMEOUT, CANCELLED, REFUNDED
    }

    public enum PaymentMethod {
        CREDIT_CARD, DEBIT_CARD, WALLET, BANK_TRANSFER, COD, PROMO_CREDIT
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED || status == PaymentStatus.DECLINED;
    }
}
