package com.fooddelivery.auth.dto;

import com.fooddelivery.auth.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding a role to a user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Add role request")
public class AddRoleRequest {

    @Schema(description = "Role to add to the user", example = "RESTAURANT_OWNER")
    @NotNull(message = "Role is required")
    private Role role;
}
