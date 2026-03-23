package com.fooddelivery.order.service;

import com.fooddelivery.order.dto.DeliveryFeeResponse;
import com.fooddelivery.order.dto.DeliveryFeeSettingsDto;
import com.fooddelivery.restaurant.entity.Restaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

/**
 * Service for calculating delivery fees dynamically based on distance, time, and other factors.
 * Settings are loaded from the database via DeliveryFeeSettingsService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryFeeCalculationService {

    private final DeliveryFeeSettingsService settingsService;

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculate delivery fee based on distance between restaurant and delivery location.
     *
     * @param restaurant        The restaurant entity
     * @param deliveryLatitude  Customer's delivery latitude
     * @param deliveryLongitude Customer's delivery longitude
     * @return Calculated delivery fee
     */
    public BigDecimal calculateDeliveryFee(Restaurant restaurant,
                                           BigDecimal deliveryLatitude,
                                           BigDecimal deliveryLongitude) {
        // Load settings from database
        DeliveryFeeSettingsDto settings = settingsService.getSettings();

        if (deliveryLatitude == null || deliveryLongitude == null) {
            log.warn("Delivery coordinates not provided, using base fee");
            return restaurant.getDeliveryFee() != null ? restaurant.getDeliveryFee() : settings.getBaseFee();
        }

        if (restaurant.getLatitude() == null || restaurant.getLongitude() == null) {
            log.warn("Restaurant coordinates not set for restaurant {}, using base fee", restaurant.getId());
            return restaurant.getDeliveryFee() != null ? restaurant.getDeliveryFee() : settings.getBaseFee();
        }

        // Calculate distance
        double distanceKm = calculateDistanceKm(
                restaurant.getLatitude().doubleValue(),
                restaurant.getLongitude().doubleValue(),
                deliveryLatitude.doubleValue(),
                deliveryLongitude.doubleValue()
        );

        log.debug("Calculated distance: {} km for restaurant {}", distanceKm, restaurant.getId());

        // Calculate base delivery fee: base + (distance * per-km rate)
        BigDecimal distanceFee = settings.getPerKmFee().multiply(BigDecimal.valueOf(distanceKm));
        BigDecimal calculatedFee = settings.getBaseFee().add(distanceFee);

        // Apply peak hour surcharge if applicable
        if (isPeakHour(settings)) {
            calculatedFee = calculatedFee.add(settings.getPeakHourSurcharge());
            log.debug("Peak hour surcharge applied: {}", settings.getPeakHourSurcharge());
        }

        // Apply minimum and maximum constraints
        if (calculatedFee.compareTo(settings.getMinFee()) < 0) {
            calculatedFee = settings.getMinFee();
        } else if (calculatedFee.compareTo(settings.getMaxFee()) > 0) {
            calculatedFee = settings.getMaxFee();
        }

        // Round to 2 decimal places
        calculatedFee = calculatedFee.setScale(2, RoundingMode.HALF_UP);

        log.info("Delivery fee calculated for restaurant {}: {} (distance: {} km)",
                restaurant.getId(), calculatedFee, String.format("%.2f", distanceKm));

        return calculatedFee;
    }

    /**
     * Calculate distance between two coordinates using Haversine formula.
     */
    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Check if current time is during peak hours.
     */
    private boolean isPeakHour(DeliveryFeeSettingsDto settings) {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();

        // Lunch peak hours
        if (hour >= settings.getPeakStartHour() && hour < settings.getPeakEndHour()) {
            return true;
        }

        // Dinner peak hours
        return hour >= settings.getEveningPeakStartHour() && hour < settings.getEveningPeakEndHour();
    }

    /**
     * Check if delivery location is within restaurant's delivery radius.
     */
    public boolean isWithinDeliveryRadius(Restaurant restaurant,
                                          BigDecimal deliveryLatitude,
                                          BigDecimal deliveryLongitude) {
        if (deliveryLatitude == null || deliveryLongitude == null) {
            return true; // Allow if coordinates not provided (will be validated elsewhere)
        }

        if (restaurant.getLatitude() == null || restaurant.getLongitude() == null) {
            return true; // Allow if restaurant coordinates not set
        }

        double distanceKm = calculateDistanceKm(
                restaurant.getLatitude().doubleValue(),
                restaurant.getLongitude().doubleValue(),
                deliveryLatitude.doubleValue(),
                deliveryLongitude.doubleValue()
        );

        Integer radiusKm = restaurant.getDeliveryRadiusKm();
        if (radiusKm == null) {
            radiusKm = 10; // Default radius
        }

        return distanceKm <= radiusKm;
    }

    /**
     * Calculate delivery fee with detailed breakdown for API response.
     *
     * @param restaurant        The restaurant entity
     * @param deliveryLatitude  Customer's delivery latitude
     * @param deliveryLongitude Customer's delivery longitude
     * @return Detailed delivery fee response
     */
    public DeliveryFeeResponse calculateDeliveryFeeDetailed(Restaurant restaurant,
                                                             BigDecimal deliveryLatitude,
                                                             BigDecimal deliveryLongitude) {
        DeliveryFeeSettingsDto settings = settingsService.getSettings();

        // Handle missing coordinates
        if (deliveryLatitude == null || deliveryLongitude == null ||
            restaurant.getLatitude() == null || restaurant.getLongitude() == null) {

            BigDecimal baseFee = restaurant.getDeliveryFee() != null ?
                    restaurant.getDeliveryFee() : settings.getBaseFee();

            return DeliveryFeeResponse.builder()
                    .deliveryFee(baseFee)
                    .distanceKm(null)
                    .withinDeliveryRadius(true)
                    .deliveryRadiusKm(restaurant.getDeliveryRadiusKm() != null ?
                            restaurant.getDeliveryRadiusKm() : 10)
                    .peakHourSurchargeApplied(false)
                    .peakHourSurcharge(BigDecimal.ZERO)
                    .build();
        }

        // Calculate distance
        double distanceKm = calculateDistanceKm(
                restaurant.getLatitude().doubleValue(),
                restaurant.getLongitude().doubleValue(),
                deliveryLatitude.doubleValue(),
                deliveryLongitude.doubleValue()
        );

        // Check delivery radius
        Integer radiusKm = restaurant.getDeliveryRadiusKm();
        if (radiusKm == null) {
            radiusKm = 10;
        }
        boolean withinRadius = distanceKm <= radiusKm;

        // Calculate fee
        BigDecimal distanceFee = settings.getPerKmFee().multiply(BigDecimal.valueOf(distanceKm));
        BigDecimal calculatedFee = settings.getBaseFee().add(distanceFee);

        // Check peak hour
        boolean isPeak = isPeakHour(settings);
        BigDecimal peakSurcharge = BigDecimal.ZERO;
        if (isPeak) {
            peakSurcharge = settings.getPeakHourSurcharge();
            calculatedFee = calculatedFee.add(peakSurcharge);
        }

        // Apply min/max constraints
        if (calculatedFee.compareTo(settings.getMinFee()) < 0) {
            calculatedFee = settings.getMinFee();
        } else if (calculatedFee.compareTo(settings.getMaxFee()) > 0) {
            calculatedFee = settings.getMaxFee();
        }

        calculatedFee = calculatedFee.setScale(2, RoundingMode.HALF_UP);

        return DeliveryFeeResponse.builder()
                .deliveryFee(calculatedFee)
                .distanceKm(Math.round(distanceKm * 100.0) / 100.0)
                .withinDeliveryRadius(withinRadius)
                .deliveryRadiusKm(radiusKm)
                .peakHourSurchargeApplied(isPeak)
                .peakHourSurcharge(peakSurcharge)
                .build();
    }
}
