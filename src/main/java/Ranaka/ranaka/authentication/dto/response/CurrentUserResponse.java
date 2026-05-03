package Ranaka.ranaka.authentication.dto.response;

import Ranaka.ranaka.user.domain.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurrentUserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Role role;
    private boolean active;
}
