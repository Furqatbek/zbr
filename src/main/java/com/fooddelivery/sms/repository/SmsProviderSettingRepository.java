package com.fooddelivery.sms.repository;

import com.fooddelivery.sms.entity.SmsProviderSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SMS provider settings.
 */
@Repository
public interface SmsProviderSettingRepository extends JpaRepository<SmsProviderSetting, Long> {

    Optional<SmsProviderSetting> findBySettingKey(String settingKey);

    List<SmsProviderSetting> findBySettingKeyStartingWith(String prefix);

    @Modifying
    @Query("UPDATE SmsProviderSetting s SET s.settingValue = :value WHERE s.settingKey = :key")
    int updateSettingValue(@Param("key") String key, @Param("value") String value);

    boolean existsBySettingKey(String settingKey);
}
