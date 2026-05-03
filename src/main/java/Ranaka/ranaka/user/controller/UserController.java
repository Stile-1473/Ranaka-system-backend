package Ranaka.ranaka.user.controller;

import Ranaka.ranaka.common.exception.ResourceNotFoundException;
import Ranaka.ranaka.user.domain.Role;
import Ranaka.ranaka.user.dto.request.ChangePasswordRequest;
import Ranaka.ranaka.user.dto.request.CreateUserRequest;
import Ranaka.ranaka.user.dto.request.UpdateUserRequest;
import Ranaka.ranaka.user.dto.response.UserResponse;
import Ranaka.ranaka.user.entity.User;
import Ranaka.ranaka.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * User Management REST Controller
 * Provides endpoints for creating, updating, and managing users.
 * Only SYSTEM_ADMIN role can access these endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class UserController {

    private final UserService userService;

    /**
     * Create a new user
     * POST /api/v1/users
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());

        try {
            User user = userService.createUser(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getPhoneNumber(),
                    request.getPassword(),
                    request.getRole()
            );

            UserResponse response = mapUserToResponse(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating user: {}", e.getMessage());
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Get all users with pagination
     * GET /api/v1/users
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(Pageable pageable) {
        log.info("Retrieving all users");

        // This shape follows the project pagination convention:
        // content + page + size + totalElements + totalPages.
        Page<User> users = userService.getAllUsers(pageable);
        Page<UserResponse> responses = users.map(this::mapUserToResponse);

        Map<String, Object> response = new HashMap<>();
        response.put("content", responses.getContent());
        response.put("page", responses.getNumber());
        response.put("size", responses.getSize());
        response.put("totalElements", responses.getTotalElements());
        response.put("totalPages", responses.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID
     * GET /api/v1/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("Retrieving user with ID: {}", id);

        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(mapUserToResponse(user));
        } catch (ResourceNotFoundException e) {
            log.error("User not found with ID: {}", id);
            throw e;
        }
    }

    /**
     * Get users by role with pagination
     * GET /api/v1/users/role/{role}
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<Map<String, Object>> getUsersByRole(@PathVariable String role, Pageable pageable) {
        log.info("Retrieving users with role: {}", role);

        try {
            Role userRole = Role.valueOf(role.toUpperCase());
            Page<User> users = userService.getUsersByRole(userRole, pageable);
            Page<UserResponse> responses = users.map(this::mapUserToResponse);

            Map<String, Object> response = new HashMap<>();
            response.put("content", responses.getContent());
            response.put("page", responses.getNumber());
            response.put("size", responses.getSize());
            response.put("totalElements", responses.getTotalElements());
            response.put("totalPages", responses.getTotalPages());
            response.put("role", role);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid role: {}", role);
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    /**
     * Update user details
     * PUT /api/v1/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("Updating user with ID: {}", id);

        try {
            User user = userService.updateUser(
                    id,
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getPhoneNumber(),
                    request.getRole()
            );

            return ResponseEntity.ok(mapUserToResponse(user));
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            log.error("Error updating user: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Change user password
     * POST /api/v1/users/{id}/change-password
     */
    @PostMapping("/{id}/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Changing password for user with ID: {}", id);

        try {
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new IllegalArgumentException("Passwords do not match");
            }

            userService.changePassword(id, request.getCurrentPassword(), request.getNewPassword());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            log.error("Error changing password: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Activate a user
     * PATCH /api/v1/users/{id}/activate
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
        log.info("Activating user with ID: {}", id);

        try {
            User user = userService.activateUser(id);
            return ResponseEntity.ok(mapUserToResponse(user));
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            log.error("Error activating user: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Deactivate a user
     * PATCH /api/v1/users/{id}/deactivate
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        log.info("Deactivating user with ID: {}", id);

        try {
            // Example reason:
            // someone left the organisation or should temporarily lose access.
            User user = userService.deactivateUser(id);
            return ResponseEntity.ok(mapUserToResponse(user));
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            log.error("Error deactivating user: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get count of active users by role
     * GET /api/v1/users/count/active/{role}
     */
    @GetMapping("/count/active/{role}")
    public ResponseEntity<Map<String, Object>> countActiveUsersByRole(@PathVariable String role) {
        log.info("Getting active user count for role: {}", role);

        try {
            Role userRole = Role.valueOf(role.toUpperCase());
            long count = userService.countActiveUsersByRole(userRole);

            Map<String, Object> response = new HashMap<>();
            response.put("role", role);
            response.put("activeCount", count);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid role: {}", role);
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private UserResponse mapUserToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .isActive(user.isActive())
                .build();
    }
}
