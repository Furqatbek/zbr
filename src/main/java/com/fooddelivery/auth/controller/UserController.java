package com.fooddelivery.auth.controller;

import com.fooddelivery.auth.dto.ChangePasswordRequest;
import com.fooddelivery.auth.dto.UpdateUserRequest;
import com.fooddelivery.auth.dto.UserDto;
import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.auth.security.UserPrincipal;
import com.fooddelivery.auth.service.AuthService;
import com.fooddelivery.auth.service.UserService;
import com.fooddelivery.common.dto.ApiResponse;
import com.fooddelivery.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user management operations.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Get the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        UserDto user = userService.getUserById(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Update the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserDto>> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateUserRequest request) {

        UserDto user = userService.updateUser(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", user));
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Change password", description = "Change password for the currently authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/me/logout-all")
    @Operation(summary = "Logout from all devices", description = "Revoke all refresh tokens for the current user")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        authService.logoutAll(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Logged out from all devices"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
    @Operation(summary = "Get user by ID", description = "Get user details by ID (Admin only)")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(
            @PathVariable Long id) {

        UserDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
    @Operation(summary = "Get all users", description = "Get all users with pagination (Admin only)")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<UserDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
    @Operation(summary = "Search users", description = "Search users by name or email (Admin only)")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> searchUsers(
            @Parameter(description = "Search query") @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<UserDto> users = userService.searchUsers(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
    @Operation(summary = "Get users by role", description = "Get users with specific role (Admin only)")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getUsersByRole(
            @PathVariable Role role,
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<UserDto> users = userService.getUsersByRole(role, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
    @Operation(summary = "Get users by status", description = "Get users with specific status (Admin only)")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getUsersByStatus(
            @PathVariable UserStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        PagedResponse<UserDto> users = userService.getUsersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLATFORM')")
    @Operation(summary = "Update user status", description = "Update user status (Admin only)")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam UserStatus status) {

        UserDto user = userService.updateUserStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("User status updated", user));
    }

    @PostMapping("/{id}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add role to user", description = "Add a role to a user (Admin only)")
    public ResponseEntity<ApiResponse<UserDto>> addRole(
            @PathVariable Long id,
            @PathVariable Role role) {

        UserDto user = userService.addRole(id, role);
        return ResponseEntity.ok(ApiResponse.success("Role added successfully", user));
    }

    @DeleteMapping("/{id}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove role from user", description = "Remove a role from a user (Admin only)")
    public ResponseEntity<ApiResponse<UserDto>> removeRole(
            @PathVariable Long id,
            @PathVariable Role role) {

        UserDto user = userService.removeRole(id, role);
        return ResponseEntity.ok(ApiResponse.success("Role removed successfully", user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Soft delete a user (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }
}
