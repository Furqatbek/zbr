package com.fooddelivery.integration.partner.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One API credential belonging to a partner.
 *
 * <p>The credential handed out is {@code zbrp_<keyId>_<secret>}. Only the
 * {@code keyId} half is stored in the clear — it is not a secret, it is indexed,
 * and it is what turns an incoming header into a single row without scanning.
 * The secret half is stored as a SHA-256 digest and cannot be recovered, so a
 * lost key is reissued rather than looked up.
 */
@Entity
@Table(name = "partner_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @Column(name = "key_id", nullable = false, length = 40, unique = true)
    private String keyId;

    @Column(name = "secret_hash", nullable = false, length = 64)
    private String secretHash;

    /**
     * Which deployment this key is for. A staging key reaching production, or
     * the reverse, is a mistake worth refusing loudly rather than letting a
     * test order print in a real kitchen.
     */
    @Column(nullable = false, length = 20)
    private String environment;

    /** Free text for humans: who was given this and why. */
    @Column(length = 200)
    private String label;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /**
     * Written by a throttled background update, never by save(), for the same
     * reason as User.lastSeenAt: a write on every request would both bump the
     * row's version and put a database write in the path of every partner call.
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public boolean isUsable() {
        return Boolean.TRUE.equals(active)
                && revokedAt == null
                && partner != null
                && Boolean.TRUE.equals(partner.getActive());
    }
}
