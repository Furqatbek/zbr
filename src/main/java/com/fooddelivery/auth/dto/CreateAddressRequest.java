package com.fooddelivery.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating a consumer address.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAddressRequest {

    @Size(max = 50, message = "Label must be at most 50 characters")
    private String label;

    @NotBlank(message = "Full address is required")
    @Size(max = 500, message = "Full address must be at most 500 characters")
    private String fullAddress;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Size(max = 50, message = "Apartment number must be at most 50 characters")
    private String apartmentNumber;

    @Size(max = 50, message = "Entrance must be at most 50 characters")
    private String entrance;

    @Size(max = 500, message = "Instructions must be at most 500 characters")
    private String instructions;
}
