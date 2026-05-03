package Ranaka.ranaka.user.service;

import Ranaka.ranaka.user.entity.User;
import Ranaka.ranaka.user.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    /**
     * Create a new user
     */
    User createUser(String firstName, String lastName, String email, String phoneNumber, 
                    String password, Role role);

    /**
     * Get user by ID
     */
    User getUserById(Long userId);

    /**
     * Get user by email
     */
    User getUserByEmail(String email);

    /**
     * Get all users with pagination
     */
    Page<User> getAllUsers(Pageable pageable);

    /**
     * Get users by role
     */
    Page<User> getUsersByRole(Role role, Pageable pageable);

    /**
     * Update user details (name, email, phone, role)
     */
    User updateUser(Long userId, String firstName, String lastName, String email, 
                    String phoneNumber, Role role);

    /**
     * Change user password
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * Activate a user
     */
    User activateUser(Long userId);

    /**
     * Deactivate a user
     */
    User deactivateUser(Long userId);

    /**
     * Check if email exists
     */
    boolean emailExists(String email);

    /**
     * Validate password strength
     */
    void validatePasswordStrength(String password);

    /**
     * Count active users by role
     */
    long countActiveUsersByRole(Role role);
}
