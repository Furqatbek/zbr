package com.fooddelivery.order.service;

import com.fooddelivery.restaurant.entity.Restaurant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

/**
 * Service for calculating delivery fees dynamically based on distance, time, and other factors.
 */
@Service
@Slf4j
public class DeliveryFeeCalculationService {

    @Value("${app.delivery.base-fee:2.00}")
    private BigDecimal baseFee;

    @Value("${app.delivery.per-km-fee:0.50}")
    private BigDecimal perKmFee;

    @Value("${app.delivery.min-fee:2.00}")
    private BigDecimal minFee;

    @Value("${app.delivery.max-fee:15.00}")
    private BigDecimal maxFee;

    @Value("${app.delivery.peak-hour-surcharge:1.50}")
    private BigDecimal peakHourSurcharge;

    @Value("${app.delivery.peak-start-hour:11}")
    private int peakStartHour;

    @Value("${app.delivery.peak-end-hour:14}")
    private int peakEndHour;

    @Value("${app.delivery.evening-peak-start-hour:18}")
    private int eveningPeakStartHour;

    @Value("${app.delivery.evening-peak-end-hour:21}")
    private int eveningPeakEndHour;

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
        if (deliveryLatitude == null || deliveryLongitude == null) {
            log.warn("Delivery coordinates not provided, using restaurant's base fee");
            return restaurant.getDeliveryFee() != null ? restaurant.getDeliveryFee() : baseFee;
        }

        if (restaurant.getLatitude() == null || restaurant.getLongitude() == null) {
            log.warn("Restaurant coordinates not set for restaurant {}, using base fee", restaurant.getId());
            return restaurant.getDeliveryFee() != null ? restaurant.getDeliveryFee() : baseFee;
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
        BigDecimal distanceFee = perKmFee.multiply(BigDecimal.valueOf(distanceKm));
        BigDecimal calculatedFee = baseFee.add(distanceFee);

        // Apply peak hour surcharge if applicable
        if (isPeakHour()) {
            calculatedFee = calculatedFee.add(peakHourSurcharge);
            log.debug("Peak hour surcharge applied: {}", peakHourSurcharge);
        }

        // Apply minimum and maximum constraints
        if (calculatedFee.compareTo(minFee) < 0) {
            calculatedFee = minFee;
        } else if (calculatedFee.compareTo(maxFee) > 0) {
            calculatedFee = maxFee;
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
    private boolean isPeakHour() {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();

        // Lunch peak hours
        if (hour >= peakStartHour && hour < peakEndHour) {
            return true;
        }

        // Dinner peak hours
        return hour >= eveningPeakStartHour && hour < eveningPeakEndHour;
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
}
