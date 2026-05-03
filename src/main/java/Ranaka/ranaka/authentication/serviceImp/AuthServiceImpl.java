package Ranaka.ranaka.authentication.serviceImp;

import Ranaka.ranaka.authentication.dto.request.RegisterRequest;
import Ranaka.ranaka.authentication.dto.request.LoginRequest;
import Ranaka.ranaka.authentication.dto.request.UpdateCurrentUserRequest;
import Ranaka.ranaka.authentication.dto.response.AuthResponse;
import Ranaka.ranaka.authentication.service.AuthService;
import Ranaka.ranaka.audit.entity.AuditLog;
import Ranaka.ranaka.audit.repository.AuditLogRepository;
import Ranaka.ranaka.common.enums.AuditAction;
import Ranaka.ranaka.common.exception.EmailAlreadyExistsException;
import Ranaka.ranaka.common.exception.InvalidCredentialsException;
import Ranaka.ranaka.common.exception.AccountInactiveException;
import Ranaka.ranaka.common.exception.ResourceNotFoundException;
import Ranaka.ranaka.security.jwt.JwtService;
import Ranaka.ranaka.user.dto.request.ChangePasswordRequest;
import Ranaka.ranaka.user.entity.User;
import Ranaka.ranaka.user.repository.UserRepository;
import Ranaka.ranaka.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;
    private final AuditLogRepository auditLogRepository;



    @Override
    public AuthResponse register(RegisterRequest request) {
        if(userRepository.existsByEmail(request.getEmail())){
            throw new EmailAlreadyExistsException("User with this email already exists");
        }

        // New users are stored with a hashed password, never with the raw password text.
        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .password(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .build();
        User savedUser = userRepository.save(user);

        return new AuthResponse(
                null,
                savedUser.getEmail(),
                "User Registered",
                savedUser.getRole()


        );
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow(
                ()-> new InvalidCredentialsException("Email does not exists")
        );

        // Human example:
        // Farai types a password on the login screen.
        // We compare that raw value to the stored bcrypt hash instead of comparing plain text.
        boolean passwordMatches = passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPassword()
        );


        if(!passwordMatches){
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if(!user.isActive()){
            throw new AccountInactiveException("This account is not active");
        }

        // The token is what the frontend will send back on later API calls.
        String token = jwtService.generateToken(user.getEmail());
        logAuditAction(user, AuditAction.LOGIN, "User logged in successfully");
        return new AuthResponse(
                token,
                user.getEmail(),
                "User logged in",
                user.getRole()

        );

    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        User currentUser = getCurrentUser();
        // We fail early here so the user gets a clear message if they mistype the confirmation field.
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        userService.changePassword(currentUser.getId(), request.getCurrentPassword(), request.getNewPassword());
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        // Depending on the security path, the principal may already be our User entity
        // or only a UserDetails wrapper. We support both paths here.
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }

        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    @Override
    public User updateCurrentUser(UpdateCurrentUserRequest request) {
        User currentUser = getCurrentUser();

        return userService.updateUser(
                currentUser.getId(),
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPhoneNumber(),
                null
        );
    }

    private void logAuditAction(User user, AuditAction action, String description) {
        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action(action)
                .description(description)
                .entityId(user.getId())
                .entityType("User")
                .newValue(user.getEmail())
                .ipAddress("system")
                .userAgent("system")
                .createdAt(LocalDateTime.now())
                .build());
    }
}
