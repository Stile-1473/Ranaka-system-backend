package Ranaka.ranaka.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PushTokenRegistrationRequest {

    @NotBlank(message = "Push token is required")
    private String token;

    @NotBlank(message = "Platform is required")
    private String platform;

    private String deviceName;
}
