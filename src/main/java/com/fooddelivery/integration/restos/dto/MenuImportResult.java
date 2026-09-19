package com.fooddelivery.integration.restos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of a menu import operation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuImportResult {
    private Long restaurantId;
    private Long externalRestaurantId;
    private int categoriesCreated;
    private int categoriesUpdated;
    private int productsCreated;
    private int productsUpdated;
    private int productsSkipped;

    /**
     * Items and categories that were live here but absent from the upstream
     * snapshot, and have been deactivated. Zero on an import
     * ({@code overwriteExisting = false}), which never retires anything.
     */
    private int productsDeactivated;
    private int categoriesDeactivated;

    private LocalDateTime syncedAt;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
