package com.fooddelivery.delivery.eta;

import com.fooddelivery.courier.entity.VehicleType;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

/**
 * How long a delivery takes, by vehicle.
 *
 * <p>Every number here is a guess about a city, so every number here is
 * configurable. The defaults are Tashkent at an ordinary hour: door-to-door
 * averages including lights and turns, not the speed a vehicle is capable of.
 * A motorcycle that can do 60 km/h averages nothing like it across town.
 *
 * <p>Speeds are deliberately pessimistic. An estimate that is too slow costs a
 * little appetite; an estimate that is too fast produces a customer watching a
 * timer that has already run out, which is the call the support line gets.
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "app.delivery.eta")
public class EtaProperties {

    /**
     * Average door-to-door speed in km/h, per vehicle.
     *
     * <p>Spring merges configured entries into this map rather than replacing
     * it, so a deployment may override one vehicle without restating the rest.
     */
    private Map<VehicleType, Double> speedKmh = new EnumMap<>(Map.of(
            VehicleType.WALKING, 4.5,
            VehicleType.BICYCLE, 12.0,
            VehicleType.E_BIKE, 18.0,
            VehicleType.MOTORCYCLE, 24.0,
            // Lower than the motorcycle on purpose: a car is faster on the road
            // and slower everywhere else — traffic it cannot filter through, and
            // somewhere to leave it at both ends.
            VehicleType.CAR, 20.0,
            VehicleType.VAN, 18.0
    ));

    /**
     * Fixed minutes per delivery that are not travel: parking, the stairs, the
     * handover at the door. Larger vehicles park further away.
     */
    private Map<VehicleType, Integer> overheadMinutes = new EnumMap<>(Map.of(
            VehicleType.WALKING, 3,
            VehicleType.BICYCLE, 3,
            VehicleType.E_BIKE, 3,
            VehicleType.MOTORCYCLE, 4,
            VehicleType.CAR, 6,
            VehicleType.VAN, 7
    ));

    /**
     * The vehicle assumed when no courier has been assigned yet — which is the
     * case for every estimate a customer sees while browsing and at checkout.
     *
     * <p>Set this to what the fleet actually rides in the city being served.
     * It defaults to the same value {@code Courier.vehicleType} defaults to, so
     * an unconfigured deployment is at least self-consistent.
     */
    private VehicleType planningVehicle = VehicleType.BICYCLE;

    /**
     * Multiplier applied to a straight-line distance to approximate the roads.
     *
     * <p>Applied only when the distance is straight-line. A route from OSRM has
     * already been round the corners and must not be inflated a second time.
     */
    private double detourFactor = 1.3;

    /** Half-width of the quoted range, as a fraction of the estimate. */
    private double spreadFraction = 0.15;

    /** Floor on the half-width, so a short delivery is not quoted to the minute. */
    private int minSpreadMinutes = 5;

    /** Quoted bounds are rounded outward to a multiple of this. */
    private int roundToMinutes = 5;

    /** Used when a vehicle has no configured speed, rather than dividing by zero. */
    static final double FALLBACK_SPEED_KMH = 12.0;

    public double speedFor(VehicleType vehicle) {
        Double speed = speedKmh.get(vehicle);
        if (speed == null || speed <= 0) {
            log.warn("No positive ETA speed configured for {}, using {} km/h", vehicle, FALLBACK_SPEED_KMH);
            return FALLBACK_SPEED_KMH;
        }
        return speed;
    }

    public int overheadFor(VehicleType vehicle) {
        Integer overhead = overheadMinutes.get(vehicle);
        return overhead == null || overhead < 0 ? 0 : overhead;
    }

    /**
     * Fails the deployment rather than the delivery.
     *
     * <p>A zero or negative speed is a divide-by-zero on the one code path every
     * customer sees, and a detour factor below 1 quotes a journey shorter than
     * the straight line between its ends.
     */
    @PostConstruct
    void validate() {
        for (VehicleType vehicle : VehicleType.values()) {
            Double speed = speedKmh.get(vehicle);
            if (speed != null && speed <= 0) {
                throw new IllegalStateException(
                        "app.delivery.eta.speed-kmh." + vehicle + " must be positive, got " + speed);
            }
        }
        if (detourFactor < 1.0) {
            throw new IllegalStateException(
                    "app.delivery.eta.detour-factor must be at least 1.0, got " + detourFactor);
        }
        if (roundToMinutes < 1) {
            throw new IllegalStateException(
                    "app.delivery.eta.round-to-minutes must be at least 1, got " + roundToMinutes);
        }
    }
}
