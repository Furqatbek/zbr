package com.fooddelivery.courier.dto;

import com.fooddelivery.courier.entity.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create courier request")
public class CreateCourierRequest {

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    private String vehicleNumber;
    private String licenseNumber;
    private Integer preferredRadiusKm;
}
