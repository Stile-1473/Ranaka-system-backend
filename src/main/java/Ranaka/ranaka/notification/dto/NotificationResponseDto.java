package Ranaka.ranaka.notification.dto;

import Ranaka.ranaka.common.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponseDto {
    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private Long referenceId;
    private String referenceType;
    private boolean read;
    private LocalDateTime readAt;
    private boolean emailSent;
    private LocalDateTime emailSentAt;
    private LocalDateTime createdAt;
}
