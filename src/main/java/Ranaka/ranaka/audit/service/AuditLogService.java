package Ranaka.ranaka.audit.service;

import Ranaka.ranaka.audit.dto.AuditLogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AuditLogService {
    Page<AuditLogResponseDto> getAuditLogs(Pageable pageable, LocalDate startDate, LocalDate endDate);
    Page<AuditLogResponseDto> getRequestAuditLogs(Long requestId, Pageable pageable);
    Page<AuditLogResponseDto> getUserAuditLogs(Long userId, Pageable pageable);
}
