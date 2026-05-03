package Ranaka.ranaka.user.entity;

import Ranaka.ranaka.user.domain.Role;
import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Builder
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @Column(nullable = false)
    private String firstName;

    @NonNull
    @Column(nullable = false)
    private String lastName;

    @Column(unique = true,nullable = false)
    @NonNull
    private String email;


    @Column(unique = true,nullable = false)
    @NonNull
    private String phoneNumber;

    @NonNull
    @Column(nullable = false)
    private String password;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;


    @Column(nullable = false)
    private boolean isActive;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security expects role names in the ROLE_XYZ format.
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        // Email is the login identity across both REST and WebSocket authentication flows.
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Deactivated users can still exist in the database, but they should not authenticate.
        return isActive;
    }
}
