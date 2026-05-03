package Ranaka.ranaka.user.serviceImpl;

import Ranaka.ranaka.audit.entity.AuditLog;
import Ranaka.ranaka.audit.repository.AuditLogRepository;
import Ranaka.ranaka.common.enums.AuditAction;
import Ranaka.ranaka.common.exception.ResourceNotFoundException;
import Ranaka.ranaka.user.domain.Role;
import Ranaka.ranaka.user.entity.User;
import Ranaka.ranaka.user.repository.UserRepository;
import Ranaka.ranaka.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    // Password validation regex - min 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[\\d\\s()+-]{7,}$"
    );

    @Override
    public User createUser(String firstName, String lastName, String email, String phoneNumber,
                          String password, Role role) {
        log.info("Creating new user with email: {}", email);

        // We validate aggressively here because user records are security-sensitive:
        // a bad email, weak password, or wrong role affects the whole system.
        // Validation
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (emailExists(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        validatePasswordStrength(password);
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }

        User user = User.builder()
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .email(email.trim().toLowerCase())
                .phoneNumber(phoneNumber.trim())
                // Passwords are always stored as hashes, never as readable text
                .password(passwordEncoder.encode(password))
                .role(role)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        logAuditAction(savedUser.getId(), "User", AuditAction.CREATE_USER,
                "User created with email: " + email, null, email);

        log.info("Successfully created user with ID: {} and email: {}", savedUser.getId(), email);
        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        log.info("Retrieving all users with pagination");
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getUsersByRole(Role role, Pageable pageable) {
        log.info("Retrieving users with role: {}", role);
        return userRepository.findByRole(role, pageable);
    }

    @Override
    public User updateUser(Long userId, String firstName, String lastName, String email,
                          String phoneNumber, Role role) {
        log.info("Updating user with ID: {}", userId);

        User user = getUserById(userId);
        String oldEmail = user.getEmail();

        // Validation
        if (firstName != null && !firstName.trim().isEmpty()) {
            user.setFirstName(firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            user.setLastName(lastName.trim());
        }
        if (email != null && !email.trim().isEmpty()) {
            String newEmail = email.trim().toLowerCase();
            if (!newEmail.equals(oldEmail) && emailExists(newEmail)) {
                throw new IllegalArgumentException("Email already exists");
            }
            if (!EMAIL_PATTERN.matcher(newEmail).matches()) {
                throw new IllegalArgumentException("Invalid email format");
            }
            user.setEmail(newEmail);
        }
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
                throw new IllegalArgumentException("Invalid phone number format");
            }
            user.setPhoneNumber(phoneNumber.trim());
        }
        if (role != null) {
            user.setRole(role);
        }

        User updatedUser = userRepository.save(user);
        logAuditAction(userId, "User", AuditAction.UPDATE_USER,
                "User details updated", oldEmail, user.getEmail());

        log.info("Successfully updated user with ID: {}", userId);
        return updatedUser;
    }


    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("Changing password for user with ID: {}", userId);

        User user = getUserById(userId);


        // the user says "My current password is X and I want to change it to Y."
        // We verify X first before accepting Y.
        // Validate old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Validate new password
        validatePasswordStrength(newPassword);

        if (oldPassword.equals(newPassword)) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        logAuditAction(userId, "User", AuditAction.UPDATE_USER,
                "Password changed", null, "Password updated");

        log.info("Successfully changed password for user with ID: {}", userId);
    }

    @Override
    public User activateUser(Long userId) {
        log.info("Activating user with ID: {}", userId);

        User user = getUserById(userId);
        if (user.isActive()) {
            throw new IllegalArgumentException("User is already active");
        }

        user.setActive(true);
        User activatedUser = userRepository.save(user);

        logAuditAction(userId, "User", AuditAction.UPDATE_USER,
                "User activated", "inactive", "active");

        log.info("Successfully activated user with ID: {}", userId);
        return activatedUser;
    }

    @Override
    public User deactivateUser(Long userId) {
        log.info("Deactivating user with ID: {}", userId);

        User user = getUserById(userId);
        if (!user.isActive()) {
            throw new IllegalArgumentException("User is already inactive");
        }

        // Deactivation is preferred over deletion so request history and audit trails stay intact
        user.setActive(false);
        User deactivatedUser = userRepository.save(user);

        logAuditAction(userId, "User", AuditAction.DEACTIVATE_USER,
                "User deactivated", "active", "inactive");

        log.info("Successfully deactivated user with ID: {}", userId);
        return deactivatedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Override
    public void validatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase letter, one lowercase letter, " +
                    "one digit, and one special character (!@#$%^&*()_+-=[]{}';:\"\\|,.<>/?)"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveUsersByRole(Role role) {
        return userRepository.countByRoleAndIsActive(role, true);
    }


    // ==================== PRIVATE HELPER METHODS ====================

    private void logAuditAction(Long userId, String entityType, AuditAction action,
                               String description, String oldValue, String newValue) {
        try {
            User auditUser = userRepository.findById(userId).orElse(null);
            AuditLog auditLog = AuditLog.builder()
                    .user(auditUser)
                    .action(action)
                    .description(description)
                    .entityType(entityType)
                    .entityId(userId)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log audit action: {}", e.getMessage());
        }
    }
}
