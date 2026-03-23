package com.fooddelivery.order.repository;

import com.fooddelivery.order.entity.DeliveryFeeSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for delivery fee settings.
 */
@Repository
public interface DeliveryFeeSettingRepository extends JpaRepository<DeliveryFeeSetting, Long> {

    Optional<DeliveryFeeSetting> findBySettingKey(String settingKey);
}
