package Ranaka.ranaka.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PushTokenRegistrationResponse {

    private final boolean registered;
    private final String platform;
    private final String deviceName;
    private final LocalDateTime registeredAt;
}
