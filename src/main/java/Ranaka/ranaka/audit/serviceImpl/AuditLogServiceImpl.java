package Ranaka.ranaka.audit.serviceImpl;

import Ranaka.ranaka.audit.dto.AuditLogResponseDto;
import Ranaka.ranaka.audit.entity.AuditLog;
import Ranaka.ranaka.audit.repository.AuditLogRepository;
import Ranaka.ranaka.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public Page<AuditLogResponseDto> getAuditLogs(Pageable pageable, LocalDate startDate, LocalDate endDate) {
        // If a date window is provided, we narrow the log search.
        // Otherwise, the system admin is asking for the broader picture.
        List<AuditLog> logs = (startDate != null && endDate != null)
                ? auditLogRepository.findByDateRange(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay().minusNanos(1))
                : auditLogRepository.findAll();

        return toPage(logs, pageable);
    }

    @Override
    public Page<AuditLogResponseDto> getRequestAuditLogs(Long requestId, Pageable pageable) {
        List<AuditLog> logs = auditLogRepository.findByEntityIdAndEntityType(requestId, "ProcurementRequest");
        return toPage(logs, pageable);
    }

    @Override
    public Page<AuditLogResponseDto> getUserAuditLogs(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDto);
    }

    private Page<AuditLogResponseDto> toPage(List<AuditLog> logs, Pageable pageable) {
        // Some repository calls return plain lists, so we paginate in memory before responding.
        List<AuditLogResponseDto> content = logs.stream()
                .sorted(Comparator.comparing(AuditLog::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .map(this::mapToDto)
                .toList();

        return new PageImpl<>(content, pageable, logs.size());
    }

    private AuditLogResponseDto mapToDto(AuditLog auditLog) {
        // We turn the raw entity into a more readable API response.
        // Example output:
        // "Tariro Ncube", action "RECOMMEND_REQUEST", entity "ProcurementRequest", createdAt "2026-04-22T10:15"
        return AuditLogResponseDto.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUser() != null ? auditLog.getUser().getId() : null)
                .userName(auditLog.getUser() != null
                        ? auditLog.getUser().getFirstName() + " " + auditLog.getUser().getLastName()
                        : "System")
                .userEmail(auditLog.getUser() != null ? auditLog.getUser().getEmail() : null)
                .action(auditLog.getAction())
                .description(auditLog.getDescription())
                .entityId(auditLog.getEntityId())
                .entityType(auditLog.getEntityType())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
