package com.fooddelivery.courier.dto;

import com.fooddelivery.courier.entity.CourierStatus;
import com.fooddelivery.courier.entity.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Courier information")
public class CourierDto {
    private Long id;
    private Long userId;
    private String userName;
    private String email;
    private String phone;
    private CourierStatus status;
    private VehicleType vehicleType;
    private String vehicleNumber;
    private BigDecimal currentLat;
    private BigDecimal currentLng;
    private Integer totalDeliveries;
    private BigDecimal averageRating;
    private Boolean verified;
    private Integer currentOrderCount;
}
