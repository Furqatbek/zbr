package com.fooddelivery.integration.partner.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * An external system integrated with the platform — a POS, an aggregator.
 *
 * <p>Separate from the credentials it holds, because a partner outlives any
 * particular key: rotating a credential must not disturb the relationship or
 * the venue grants hanging off it.
 */
@Entity
@Table(name = "partners")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable machine name, e.g. RESTOS. Used in logs and rate-limit keys. */
    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Where we POST orders for this partner. Null means we push nothing to
     * them, whatever a venue grant says — a venue switched on against a partner
     * with no address configured is a misconfiguration, not an instruction.
     */
    @Column(name = "outbound_base_url", length = 500)
    private String outboundBaseUrl;

    /**
     * The credential THEY issued US. Distinct from the keys we issue them, and
     * never returned by any endpoint.
     */
    @Column(name = "outbound_api_key", length = 500)
    private String outboundApiKey;

    /**
     * Which header carries that credential. Restos accept X-Partner-Key or
     * Authorization; the next partner will want something else, and hard-coding
     * one means a code change per integration. Null means Authorization: Bearer.
     */
    @Column(name = "outbound_auth_header", length = 50)
    private String outboundAuthHeader;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
