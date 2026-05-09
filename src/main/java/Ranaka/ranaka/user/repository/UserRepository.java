package Ranaka.ranaka.user.repository;

import Ranaka.ranaka.user.domain.Role;
import Ranaka.ranaka.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    /**
     * Find a user by their email address
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email
     */
    boolean existsByEmail(String email);

    /**
     * Find a user by phone number so bootstrap/admin flows can avoid unique collisions.
     */
    Optional<User> findByPhoneNumber(String phoneNumber);

    /**
     * Find all users with a specific role
     * Used for sending notifications to users by their role (ADMIN, GM, CEO, REQUESTER)
     *
     * @param role The role to filter users by
     * @return List of all users with the specified role
     */
    List<User> findByRole(Role role);

    /**
     * Find users with a specific role with pagination support
     */
    Page<User> findByRole(Role role, Pageable pageable);

    /**
     * Count active users by role
     */
    long countByRoleAndIsActive(Role role, boolean isActive);

}
