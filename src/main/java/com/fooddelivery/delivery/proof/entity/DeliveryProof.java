package com.fooddelivery.delivery.proof.entity;

import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Proof of delivery record with photo, location, and signature.
 */
@Entity
@Table(name = "delivery_proofs", indexes = {
        @Index(name = "idx_dp_order_id", columnList = "order_id"),
        @Index(name = "idx_dp_courier_id", columnList = "courier_id"),
        @Index(name = "idx_dp_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryProof {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id", nullable = false)
    private Courier courier;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "photo_hash", length = 64)
    private String photoHash;

    @Column(name = "signature_url", length = 500)
    private String signatureUrl;

    @Column(name = "delivery_latitude", precision = 10, scale = 7)
    private BigDecimal deliveryLatitude;

    @Column(name = "delivery_longitude", precision = 10, scale = 7)
    private BigDecimal deliveryLongitude;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(name = "delivery_notes", length = 500)
    private String deliveryNotes;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void markVerified() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }
}
