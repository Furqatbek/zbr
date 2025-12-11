package com.fooddelivery.restaurant.dto;

import com.fooddelivery.restaurant.entity.RestaurantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO for restaurant information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Restaurant information")
public class RestaurantDto {

    @Schema(description = "Restaurant ID")
    private Long id;

    @Schema(description = "Owner user ID")
    private Long ownerId;

    @Schema(description = "Restaurant name")
    private String name;

    @Schema(description = "URL-friendly slug")
    private String slug;

    @Schema(description = "Restaurant description")
    private String description;

    @Schema(description = "Logo URL")
    private String logoUrl;

    @Schema(description = "Cover image URL")
    private String coverImageUrl;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Full address")
    private String fullAddress;

    @Schema(description = "Address line 1")
    private String addressLine1;

    @Schema(description = "City")
    private String city;

    @Schema(description = "State/Province")
    private String state;

    @Schema(description = "Postal code")
    private String postalCode;

    @Schema(description = "Country")
    private String country;

    @Schema(description = "Latitude")
    private BigDecimal latitude;

    @Schema(description = "Longitude")
    private BigDecimal longitude;

    @Schema(description = "Restaurant status")
    private RestaurantStatus status;

    @Schema(description = "Is featured restaurant")
    private Boolean featured;

    @Schema(description = "Accepts delivery orders")
    private Boolean acceptsDelivery;

    @Schema(description = "Accepts takeaway orders")
    private Boolean acceptsTakeaway;

    @Schema(description = "Accepts dine-in orders")
    private Boolean acceptsDineIn;

    @Schema(description = "Minimum order amount")
    private BigDecimal minimumOrder;

    @Schema(description = "Delivery fee")
    private BigDecimal deliveryFee;

    @Schema(description = "Delivery radius in km")
    private Integer deliveryRadiusKm;

    @Schema(description = "Average preparation time in minutes")
    private Integer averagePrepTimeMinutes;

    @Schema(description = "Opening time")
    private LocalTime opensAt;

    @Schema(description = "Closing time")
    private LocalTime closesAt;

    @Schema(description = "Is currently open")
    private Boolean isOpen;

    @Schema(description = "Is currently accepting orders")
    private Boolean isCurrentlyOpen;

    @Schema(description = "Average rating (0-5)")
    private BigDecimal averageRating;

    @Schema(description = "Total number of ratings")
    private Integer totalRatings;

    @Schema(description = "Total number of orders")
    private Integer totalOrders;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;
}
