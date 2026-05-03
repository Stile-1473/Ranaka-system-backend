package Ranaka.ranaka.authentication.controller;

import Ranaka.ranaka.authentication.dto.request.LoginRequest;
import Ranaka.ranaka.authentication.dto.request.RegisterRequest;
import Ranaka.ranaka.authentication.dto.request.UpdateCurrentUserRequest;
import Ranaka.ranaka.authentication.dto.response.AuthResponse;
import Ranaka.ranaka.authentication.dto.response.CurrentUserResponse;
import Ranaka.ranaka.authentication.service.AuthService;
import Ranaka.ranaka.user.dto.request.ChangePasswordRequest;
import Ranaka.ranaka.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // Registration is restricted because user creation is an administrative action in this system.
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        // Useful for frontend bootstrapping:
        // "I already have a token, but who am I and what role do I have?"
        User user = authService.getCurrentUser();
        CurrentUserResponse response = CurrentUserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .active(user.isActive())
                .build();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CurrentUserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateCurrentUserRequest request) {
        User user = authService.updateCurrentUser(request);
        CurrentUserResponse response = CurrentUserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .active(user.isActive())
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        // The authenticated user changes their own password here.
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}
