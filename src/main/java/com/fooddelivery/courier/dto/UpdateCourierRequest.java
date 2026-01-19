package com.fooddelivery.courier.dto;

import com.fooddelivery.courier.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating courier profile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCourierRequest {

    private VehicleType vehicleType;

    private String vehicleNumber;

    private String licenseNumber;

    private Integer preferredRadiusKm;

    private Integer maxConcurrentOrders;
}
