package com.fooddelivery.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Request DTO for updating a restaurant. All fields are optional — only provided fields are updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update restaurant request — all fields optional")
public class UpdateRestaurantRequest {

    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    private String addressLine2;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    private Boolean acceptsDelivery;
    private Boolean acceptsTakeaway;
    private Boolean acceptsDineIn;

    @DecimalMin(value = "0.0", message = "Minimum order must be non-negative")
    private BigDecimal minimumOrder;

    @DecimalMin(value = "0.0", message = "Delivery fee must be non-negative")
    private BigDecimal deliveryFee;

    @Min(value = 1, message = "Delivery radius must be at least 1 km")
    private Integer deliveryRadiusKm;

    @Min(value = 5, message = "Prep time must be at least 5 minutes")
    @Max(value = 180, message = "Prep time must not exceed 180 minutes")
    private Integer averagePrepTimeMinutes;

    private LocalTime opensAt;
    private LocalTime closesAt;
}
