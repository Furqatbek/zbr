package com.fooddelivery.integration.partner.repository;

import com.fooddelivery.integration.partner.entity.PartnerOrderPush;
import com.fooddelivery.integration.partner.entity.PartnerPushStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartnerOrderPushRepository extends JpaRepository<PartnerOrderPush, Long> {

    Optional<PartnerOrderPush> findByOrderIdAndPartnerId(Long orderId, Long partnerId);

    List<PartnerOrderPush> findByStatusOrderByCreatedAtAsc(PartnerPushStatus status);

    List<PartnerOrderPush> findByOrderId(Long orderId);
}
