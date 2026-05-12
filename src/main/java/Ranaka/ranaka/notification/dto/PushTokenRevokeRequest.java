package Ranaka.ranaka.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PushTokenRevokeRequest {

    @NotBlank(message = "Push token is required")
    private String token;
}
