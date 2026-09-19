package com.fooddelivery.integration.partner.repository;

import com.fooddelivery.integration.partner.entity.PartnerVenueGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartnerVenueGrantRepository extends JpaRepository<PartnerVenueGrant, Long> {

    Optional<PartnerVenueGrant> findByPartnerIdAndExternalVenueId(Long partnerId, String externalVenueId);

    Optional<PartnerVenueGrant> findByPartnerIdAndRestaurantId(Long partnerId, Long restaurantId);

    List<PartnerVenueGrant> findByPartnerId(Long partnerId);

    /**
     * The partner this restaurant's orders are cooked from, if any.
     *
     * <p>Optional rather than a list because a kitchen has one till. Two
     * partners both printing the same order is two kitchens cooking it, so the
     * unique index on (partner, restaurant) plus this signature is the shape
     * that keeps it impossible rather than merely unlikely.
     */
    Optional<PartnerVenueGrant> findByRestaurantIdAndPushOrdersTrue(Long restaurantId);
}
