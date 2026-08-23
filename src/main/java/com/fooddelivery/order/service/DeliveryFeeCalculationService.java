package com.fooddelivery.order.service;

import com.fooddelivery.order.dto.DeliveryFeeResponse;
import com.fooddelivery.order.dto.DeliveryFeeSettingsDto;
import com.fooddelivery.restaurant.entity.Restaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Service for calculating delivery fees dynamically based on distance, time, and other factors.
 * Settings are loaded from the database via DeliveryFeeSettingsService.
 * Distance calculation uses OSRM route distance when enabled, with Haversine fallback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryFeeCalculationService {

    private final DeliveryFeeSettingsService settingsService;
    private final RouteDistanceService routeDistanceService;

    /**
     * Initialised as well as injected: Spring overwrites this, but @Value is
     * not honoured by Mockito's @InjectMocks, and a null zone would throw at
     * fee calculation — the one code path every order goes through.
     */
    private ZoneId businessZone = ZoneId.of("Asia/Tashkent");

    @Value("${app.timezone:Asia/Tashkent}")
    void setBusinessZone(String zone) {
        this.businessZone = ZoneId.of(zone);
    }

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
            log.warn("Delivery coordinates not provided for restaurant {}, using base fee from settings", restaurant.getId());
            return settings.getBaseFee();
        }

        if (restaurant.getLatitude() == null || restaurant.getLongitude() == null) {
            log.warn("Restaurant coordinates not set for restaurant {}, using base fee from settings", restaurant.getId());
            return settings.getBaseFee();
        }

        // Calculate distance using route-based calculation (with Haversine fallback)
        double distanceKm = routeDistanceService.calculateDistanceKm(
                restaurant.getLatitude().doubleValue(),
                restaurant.getLongitude().doubleValue(),
                deliveryLatitude.doubleValue(),
                deliveryLongitude.doubleValue()
        );

        log.debug("Calculated distance: {} km for restaurant {}", distanceKm, restaurant.getId());

        // Calculate base delivery fee: base + (distance * per-km rate)
        BigDecimal distanceFee = settings.getPerKmFee().multiply(BigDecimal.valueOf(distanceKm));
        BigDecimal calculatedFee = settings.getBaseFee().add(distanceFee);

        log.info("Delivery fee breakdown for restaurant {}: baseFee={} + perKmFee={} x distance={} km = {}",
                restaurant.getId(), settings.getBaseFee(), settings.getPerKmFee(),
                String.format("%.2f", distanceKm), calculatedFee);

        // Apply peak hour surcharge if applicable
        if (isPeakHour(settings)) {
            calculatedFee = calculatedFee.add(settings.getPeakHourSurcharge());
            log.info("Peak hour surcharge applied: {} -> total: {}", settings.getPeakHourSurcharge(), calculatedFee);
        }

        // Apply minimum and maximum constraints
        if (calculatedFee.compareTo(settings.getMinFee()) < 0) {
            log.info("Fee {} below minimum {}, using minimum", calculatedFee, settings.getMinFee());
            calculatedFee = settings.getMinFee();
        } else if (calculatedFee.compareTo(settings.getMaxFee()) > 0) {
            log.info("Fee {} above maximum {}, capping to maximum", calculatedFee, settings.getMaxFee());
            calculatedFee = settings.getMaxFee();
        }

        // Round to 2 decimal places
        calculatedFee = calculatedFee.setScale(2, RoundingMode.HALF_UP);

        return calculatedFee;
    }

    /**
     * Check if current time is during peak hours.
     *
     * <p>Evaluated in the business timezone, NOT the JVM's. The JVM is pinned
     * to UTC so timestamps store consistently; reading the clock directly here
     * put the 11–14 lunch window at 16:00–19:00 Tashkent and the 18–21 dinner
     * window at 23:00–02:00, so the surcharge missed both real rushes.
     */
    private boolean isPeakHour(DeliveryFeeSettingsDto settings) {
        LocalTime now = LocalTime.now(businessZone);
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

        double distanceKm = routeDistanceService.calculateDistanceKm(
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

        // Handle missing coordinates - use base fee from settings (not restaurant static fee)
        if (deliveryLatitude == null || deliveryLongitude == null ||
            restaurant.getLatitude() == null || restaurant.getLongitude() == null) {

            log.warn("Missing coordinates for fee calculation (delivery={}/{}, restaurant={}/{}), using base fee from settings",
                    deliveryLatitude, deliveryLongitude, restaurant.getLatitude(), restaurant.getLongitude());

            return DeliveryFeeResponse.builder()
                    .deliveryFee(settings.getBaseFee())
                    .baseFee(settings.getBaseFee())
                    .perKmFee(settings.getPerKmFee())
                    .distanceFee(BigDecimal.ZERO)
                    .distanceKm(null)
                    .withinDeliveryRadius(true)
                    .deliveryRadiusKm(restaurant.getDeliveryRadiusKm() != null ?
                            restaurant.getDeliveryRadiusKm() : 10)
                    .peakHourSurchargeApplied(false)
                    .peakHourSurcharge(BigDecimal.ZERO)
                    .build();
        }

        // Calculate distance using route-based calculation (with Haversine fallback)
        double distanceKm = routeDistanceService.calculateDistanceKm(
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
            log.info("Fee {} below minimum {}, using minimum", calculatedFee, settings.getMinFee());
            calculatedFee = settings.getMinFee();
        } else if (calculatedFee.compareTo(settings.getMaxFee()) > 0) {
            log.info("Fee {} above maximum {}, capping to maximum", calculatedFee, settings.getMaxFee());
            calculatedFee = settings.getMaxFee();
        }

        calculatedFee = calculatedFee.setScale(2, RoundingMode.HALF_UP);

        return DeliveryFeeResponse.builder()
                .deliveryFee(calculatedFee)
                .baseFee(settings.getBaseFee())
                .perKmFee(settings.getPerKmFee())
                .distanceFee(distanceFee)
                .distanceKm(Math.round(distanceKm * 100.0) / 100.0)
                .withinDeliveryRadius(withinRadius)
                .deliveryRadiusKm(radiusKm)
                .peakHourSurchargeApplied(isPeak)
                .peakHourSurcharge(peakSurcharge)
                .minFee(settings.getMinFee())
                .maxFee(settings.getMaxFee())
                .build();
    }
}
