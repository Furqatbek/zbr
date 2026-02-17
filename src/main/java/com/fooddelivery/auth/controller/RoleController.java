package com.fooddelivery.auth.controller;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for role reference data.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles", description = "Role reference data endpoints")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    /**
     * Get all available user roles.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
    @Operation(summary = "Get all roles", description = "Get all available user roles with their display names")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getAllRoles() {
        log.debug("Getting all user roles");

        List<Map<String, String>> roles = Arrays.stream(Role.values())
                .map(role -> Map.of(
                        "value", role.name(),
                        "displayName", formatDisplayName(role.name())
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    /**
     * Format role name as display name.
     */
    private String formatDisplayName(String roleName) {
        return Arrays.stream(roleName.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
