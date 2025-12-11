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
 * Request DTO for creating a new restaurant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create restaurant request")
public class CreateRestaurantRequest {

    @Schema(description = "Restaurant name", example = "Pizza Palace", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Restaurant name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Schema(description = "Restaurant description", example = "Best pizza in town!")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Schema(description = "Phone number", example = "+1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phone;

    @Schema(description = "Email address", example = "contact@pizzapalace.com")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Address line 1", example = "123 Main Street", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String addressLine1;

    @Schema(description = "Address line 2", example = "Suite 100")
    @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    private String addressLine2;

    @Schema(description = "City", example = "New York", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Schema(description = "State/Province", example = "NY")
    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Schema(description = "Postal code", example = "10001")
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @Schema(description = "Country", example = "USA")
    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @Schema(description = "Latitude", example = "40.7128")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @Schema(description = "Longitude", example = "-74.0060")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    @Schema(description = "Accepts delivery orders", example = "true")
    @Builder.Default
    private Boolean acceptsDelivery = true;

    @Schema(description = "Accepts takeaway orders", example = "true")
    @Builder.Default
    private Boolean acceptsTakeaway = true;

    @Schema(description = "Accepts dine-in orders", example = "false")
    @Builder.Default
    private Boolean acceptsDineIn = false;

    @Schema(description = "Minimum order amount", example = "15.00")
    @DecimalMin(value = "0.0", message = "Minimum order must be non-negative")
    private BigDecimal minimumOrder;

    @Schema(description = "Delivery fee", example = "3.50")
    @DecimalMin(value = "0.0", message = "Delivery fee must be non-negative")
    private BigDecimal deliveryFee;

    @Schema(description = "Delivery radius in km", example = "10")
    @Min(value = 1, message = "Delivery radius must be at least 1 km")
    @Max(value = 50, message = "Delivery radius must not exceed 50 km")
    private Integer deliveryRadiusKm;

    @Schema(description = "Average preparation time in minutes", example = "30")
    @Min(value = 5, message = "Prep time must be at least 5 minutes")
    @Max(value = 180, message = "Prep time must not exceed 180 minutes")
    private Integer averagePrepTimeMinutes;

    @Schema(description = "Opening time", example = "09:00")
    private LocalTime opensAt;

    @Schema(description = "Closing time", example = "22:00")
    private LocalTime closesAt;
}
