package Ranaka.ranaka.audit.dto;

import Ranaka.ranaka.common.enums.AuditAction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponseDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private AuditAction action;
    private String description;
    private Long entityId;
    private String entityType;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
