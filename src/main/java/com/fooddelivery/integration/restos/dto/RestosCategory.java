package com.fooddelivery.integration.restos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Category DTO from Restos API.
 * Used by endpoints #1, #3, #4, #5.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestosCategory {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Integer sortOrder;
    private Boolean active;

    // From endpoint #5 (categories with kitchen station info)
    private Long kitchenStationId;
    private String kitchenStationName;

    // Nested products (from endpoints #3, #4)
    @Builder.Default
    private List<RestosProduct> products = new ArrayList<>();
}
