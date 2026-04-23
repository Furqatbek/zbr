package com.fooddelivery.integration.restos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO to trigger menu import from Restos system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuImportRequest {

    @NotBlank(message = "Restos system base URL is required")
    private String baseUrl;

    @NotNull(message = "External restaurant ID is required")
    private Long externalRestaurantId;

    private String apiKey;

    @Builder.Default
    private Boolean overwriteExisting = false;
}
