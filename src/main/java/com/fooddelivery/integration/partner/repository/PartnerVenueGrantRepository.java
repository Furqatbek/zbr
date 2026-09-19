package com.fooddelivery.integration.partner.repository;

import com.fooddelivery.integration.partner.entity.PartnerVenueGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartnerVenueGrantRepository extends JpaRepository<PartnerVenueGrant, Long> {

    Optional<PartnerVenueGrant> findByPartnerIdAndExternalVenueId(Long partnerId, String externalVenueId);

    Optional<PartnerVenueGrant> findByPartnerIdAndRestaurantId(Long partnerId, Long restaurantId);

    List<PartnerVenueGrant> findByPartnerId(Long partnerId);
}
