package com.fooddelivery.order.service;

import com.fooddelivery.order.dto.DeliveryFeeSettingDto;
import com.fooddelivery.order.dto.DeliveryFeeSettingsDto;
import com.fooddelivery.order.dto.UpdateDeliveryFeeSettingsRequest;
import com.fooddelivery.order.entity.DeliveryFeeSetting;
import com.fooddelivery.order.repository.DeliveryFeeSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing delivery fee settings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryFeeSettingsService {

    private final DeliveryFeeSettingRepository repository;

    // Setting keys
    public static final String BASE_FEE = "BASE_FEE";
    public static final String PER_KM_FEE = "PER_KM_FEE";
    public static final String MIN_FEE = "MIN_FEE";
    public static final String MAX_FEE = "MAX_FEE";
    public static final String PEAK_HOUR_SURCHARGE = "PEAK_HOUR_SURCHARGE";
    public static final String PEAK_START_HOUR = "PEAK_START_HOUR";
    public static final String PEAK_END_HOUR = "PEAK_END_HOUR";
    public static final String EVENING_PEAK_START_HOUR = "EVENING_PEAK_START_HOUR";
    public static final String EVENING_PEAK_END_HOUR = "EVENING_PEAK_END_HOUR";

    // Default values
    private static final BigDecimal DEFAULT_BASE_FEE = new BigDecimal("2.00");
    private static final BigDecimal DEFAULT_PER_KM_FEE = new BigDecimal("0.50");
    private static final BigDecimal DEFAULT_MIN_FEE = new BigDecimal("2.00");
    private static final BigDecimal DEFAULT_MAX_FEE = new BigDecimal("15.00");
    private static final BigDecimal DEFAULT_PEAK_SURCHARGE = new BigDecimal("1.50");
    private static final int DEFAULT_PEAK_START = 11;
    private static final int DEFAULT_PEAK_END = 14;
    private static final int DEFAULT_EVENING_PEAK_START = 18;
    private static final int DEFAULT_EVENING_PEAK_END = 21;

    /**
     * Get all delivery fee settings as a consolidated DTO.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "deliveryFeeSettings", key = "'all'")
    public DeliveryFeeSettingsDto getSettings() {
        return DeliveryFeeSettingsDto.builder()
                .baseFee(getSettingValue(BASE_FEE, DEFAULT_BASE_FEE))
                .perKmFee(getSettingValue(PER_KM_FEE, DEFAULT_PER_KM_FEE))
                .minFee(getSettingValue(MIN_FEE, DEFAULT_MIN_FEE))
                .maxFee(getSettingValue(MAX_FEE, DEFAULT_MAX_FEE))
                .peakHourSurcharge(getSettingValue(PEAK_HOUR_SURCHARGE, DEFAULT_PEAK_SURCHARGE))
                .peakStartHour(getSettingValue(PEAK_START_HOUR, BigDecimal.valueOf(DEFAULT_PEAK_START)).intValue())
                .peakEndHour(getSettingValue(PEAK_END_HOUR, BigDecimal.valueOf(DEFAULT_PEAK_END)).intValue())
                .eveningPeakStartHour(getSettingValue(EVENING_PEAK_START_HOUR, BigDecimal.valueOf(DEFAULT_EVENING_PEAK_START)).intValue())
                .eveningPeakEndHour(getSettingValue(EVENING_PEAK_END_HOUR, BigDecimal.valueOf(DEFAULT_EVENING_PEAK_END)).intValue())
                .build();
    }

    /**
     * Get all individual settings.
     */
    @Transactional(readOnly = true)
    public List<DeliveryFeeSettingDto> getAllSettings() {
        return repository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Update delivery fee settings.
     */
    @Transactional
    @CacheEvict(value = "deliveryFeeSettings", allEntries = true)
    public DeliveryFeeSettingsDto updateSettings(UpdateDeliveryFeeSettingsRequest request) {
        log.info("Updating delivery fee settings");

        if (request.getBaseFee() != null) {
            updateSetting(BASE_FEE, request.getBaseFee());
        }
        if (request.getPerKmFee() != null) {
            updateSetting(PER_KM_FEE, request.getPerKmFee());
        }
        if (request.getMinFee() != null) {
            updateSetting(MIN_FEE, request.getMinFee());
        }
        if (request.getMaxFee() != null) {
            updateSetting(MAX_FEE, request.getMaxFee());
        }
        if (request.getPeakHourSurcharge() != null) {
            updateSetting(PEAK_HOUR_SURCHARGE, request.getPeakHourSurcharge());
        }
        if (request.getPeakStartHour() != null) {
            updateSetting(PEAK_START_HOUR, BigDecimal.valueOf(request.getPeakStartHour()));
        }
        if (request.getPeakEndHour() != null) {
            updateSetting(PEAK_END_HOUR, BigDecimal.valueOf(request.getPeakEndHour()));
        }
        if (request.getEveningPeakStartHour() != null) {
            updateSetting(EVENING_PEAK_START_HOUR, BigDecimal.valueOf(request.getEveningPeakStartHour()));
        }
        if (request.getEveningPeakEndHour() != null) {
            updateSetting(EVENING_PEAK_END_HOUR, BigDecimal.valueOf(request.getEveningPeakEndHour()));
        }

        log.info("Delivery fee settings updated successfully");
        return getSettings();
    }

    /**
     * Update a single setting by key.
     */
    @Transactional
    @CacheEvict(value = "deliveryFeeSettings", allEntries = true)
    public DeliveryFeeSettingDto updateSingleSetting(String key, BigDecimal value) {
        DeliveryFeeSetting setting = repository.findBySettingKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Setting not found: " + key));

        setting.setSettingValue(value);
        setting = repository.save(setting);

        log.info("Updated delivery fee setting: {} = {}", key, value);
        return toDto(setting);
    }

    /**
     * Get a setting value by key.
     */
    public BigDecimal getSettingValue(String key, BigDecimal defaultValue) {
        return repository.findBySettingKey(key)
                .map(DeliveryFeeSetting::getSettingValue)
                .orElse(defaultValue);
    }

    private void updateSetting(String key, BigDecimal value) {
        repository.findBySettingKey(key).ifPresentOrElse(
                setting -> {
                    setting.setSettingValue(value);
                    repository.save(setting);
                },
                () -> {
                    DeliveryFeeSetting newSetting = DeliveryFeeSetting.builder()
                            .settingKey(key)
                            .settingValue(value)
                            .build();
                    repository.save(newSetting);
                }
        );
    }

    private DeliveryFeeSettingDto toDto(DeliveryFeeSetting entity) {
        return DeliveryFeeSettingDto.builder()
                .id(entity.getId())
                .settingKey(entity.getSettingKey())
                .settingValue(entity.getSettingValue())
                .description(entity.getDescription())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
