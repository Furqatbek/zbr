package com.fooddelivery.integration.partner.entity;

import com.fooddelivery.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A partner's permission over one restaurant, and the translation between their
 * venue id and ours.
 *
 * <p>Partner requests address venues by {@code externalVenueId} so they never
 * have to store our identifiers — the thing Restos asked us to hold to, and
 * much cheaper to honour now than to retrofit after a year of orders.
 */
@Entity
@Table(name = "partner_venue_grants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerVenueGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "external_venue_id", nullable = false, length = 100)
    private String externalVenueId;

    /**
     * Empty by default, and deliberately so: creating the mapping between their
     * venue and ours is not the same decision as letting them write into it.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "partner_venue_grant_capabilities",
            joinColumns = @JoinColumn(name = "grant_id"))
    @Column(name = "capability", length = 40)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<PartnerCapability> capabilities = new HashSet<>();

    /**
     * Whether this venue's orders are sent to the partner's till.
     *
     * <p>Off by default and separate from every capability above, because those
     * describe what the partner may do to us and this decides where a
     * restaurant's orders get cooked. Switching it on for the wrong venue means
     * a ticket printing in the wrong kitchen; switching it on before the
     * partner is ready means orders nobody cooks.
     */
    @Column(name = "push_orders", nullable = false)
    @Builder.Default
    private Boolean pushOrders = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isPushOrders() {
        return Boolean.TRUE.equals(pushOrders);
    }

    public boolean allows(PartnerCapability capability) {
        return capabilities != null && capabilities.contains(capability);
    }
}
