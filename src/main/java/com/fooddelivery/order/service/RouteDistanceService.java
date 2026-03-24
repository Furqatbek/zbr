package com.fooddelivery.order.service;

import com.fooddelivery.order.config.RoutingProperties;
import com.fooddelivery.order.dto.DeliveryFeeSettingsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Service for calculating route-based distance using OSRM (Open Source Routing Machine).
 * Falls back to Haversine (straight-line) calculation when OSRM is unavailable or disabled.
 *
 * Routing settings are read from the database (via DeliveryFeeSettingsService) at runtime,
 * with fallback to application.yml properties. This allows admins to toggle routing on/off
 * and change the OSRM URL via the delivery fee settings API without restarting the service.
 */
@Service
@Slf4j
public class RouteDistanceService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final DeliveryFeeSettingsService settingsService;
    private final RoutingProperties properties;
    private final RestTemplate routingRestTemplate;

    public RouteDistanceService(DeliveryFeeSettingsService settingsService,
                                RoutingProperties properties,
                                @Qualifier("routingRestTemplate") RestTemplate routingRestTemplate) {
        this.settingsService = settingsService;
        this.properties = properties;
        this.routingRestTemplate = routingRestTemplate;
    }

    /**
     * Calculate distance in km between two coordinates.
     * Uses OSRM route distance if enabled and available, otherwise falls back to Haversine.
     *
     * @return distance in kilometers
     */
    public double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        DeliveryFeeSettingsDto settings = settingsService.getSettings();
        boolean routingEnabled = settings.getRoutingEnabled() != null
                ? settings.getRoutingEnabled()
                : properties.isEnabled();

        if (!routingEnabled) {
            return calculateHaversineDistanceKm(lat1, lon1, lat2, lon2);
        }

        String osrmBaseUrl = settings.getRoutingOsrmBaseUrl() != null
                ? settings.getRoutingOsrmBaseUrl()
                : properties.getOsrmBaseUrl();

        try {
            double routeDistance = fetchRouteDistanceKm(osrmBaseUrl, lat1, lon1, lat2, lon2);
            log.debug("OSRM route distance: {} km (straight-line: {} km)",
                    String.format("%.2f", routeDistance),
                    String.format("%.2f", calculateHaversineDistanceKm(lat1, lon1, lat2, lon2)));
            return routeDistance;
        } catch (Exception e) {
            log.warn("OSRM route calculation failed, falling back to Haversine: {}", e.getMessage());
            return calculateHaversineDistanceKm(lat1, lon1, lat2, lon2);
        }
    }

    /**
     * Fetch route distance from OSRM API.
     * OSRM expects coordinates as lon,lat (not lat,lon).
     * API docs: http://project-osrm.org/docs/v5.24.0/api/#route-service
     */
    @SuppressWarnings("unchecked")
    private double fetchRouteDistanceKm(String osrmBaseUrl, double lat1, double lon1, double lat2, double lon2) {
        String url = String.format("%s/route/v1/driving/%f,%f;%f,%f?overview=false",
                osrmBaseUrl, lon1, lat1, lon2, lat2);

        ResponseEntity<Map<String, Object>> response = routingRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new RuntimeException("OSRM returned empty response");
        }

        String code = (String) body.get("code");
        if (!"Ok".equals(code)) {
            throw new RuntimeException("OSRM returned error code: " + code);
        }

        List<Map<String, Object>> routes = (List<Map<String, Object>>) body.get("routes");
        if (routes == null || routes.isEmpty()) {
            throw new RuntimeException("OSRM returned no routes");
        }

        // OSRM returns distance in meters
        Number distanceMeters = (Number) routes.get(0).get("distance");
        if (distanceMeters == null) {
            throw new RuntimeException("OSRM route has no distance field");
        }

        return distanceMeters.doubleValue() / 1000.0;
    }

    /**
     * Calculate straight-line distance using Haversine formula (fallback).
     */
    public double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
