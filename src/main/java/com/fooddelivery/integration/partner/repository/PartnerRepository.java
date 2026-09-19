package com.fooddelivery.integration.partner.repository;

import com.fooddelivery.integration.partner.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartnerRepository extends JpaRepository<Partner, Long> {
    Optional<Partner> findByCode(String code);
    boolean existsByCode(String code);
}
