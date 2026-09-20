package com.fooddelivery.delivery.eta;

import com.fooddelivery.courier.entity.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * How far away the food is and how long it should take to arrive.
 *
 * <p>Carries its own provenance — which vehicle it assumed and whether the
 * distance came from a road network or a straight line — because every consumer
 * of this has had to ask, and the answer decides whether the number is worth
 * showing to the minute.
 */
@Value
@Builder
@Schema(description = "Distance and delivery-time estimate")
public class DeliveryEta {

    @Schema(description = "Distance from the restaurant to the customer, in km")
    Double distanceKm;

    @Schema(description = "Kitchen time in minutes")
    int prepMinutes;

    @Schema(description = "Courier time in minutes: travel plus parking and handover")
    int travelMinutes;

    @Schema(description = "Lower bound of the quoted range, in minutes from now")
    int etaMinutesMin;

    @Schema(description = "Upper bound of the quoted range, in minutes from now")
    int etaMinutesMax;

    @Schema(description = "The vehicle this estimate was computed for")
    VehicleType vehicle;

    @Schema(description = "True when no courier was assigned yet and the vehicle is the configured assumption")
    boolean vehicleAssumed;

    @Schema(description = "True when the distance is a road route, false when it is straight-line times the detour factor")
    boolean routed;
}
