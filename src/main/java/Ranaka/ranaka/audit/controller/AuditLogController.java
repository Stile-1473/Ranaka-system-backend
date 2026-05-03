package Ranaka.ranaka.audit.controller;

import Ranaka.ranaka.audit.dto.AuditLogResponseDto;
import Ranaka.ranaka.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponseDto>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // This endpoint is for broad audit review, for example:
        // "Show me everything important that happened this week."
        return ResponseEntity.ok(
                auditLogService.getAuditLogs(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")), startDate, endDate)
        );
    }

    @GetMapping("/request/{requestId}")
    public ResponseEntity<Page<AuditLogResponseDto>> getRequestAuditLogs(
            @PathVariable Long requestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Helpful when leadership or support asks:
        // "Walk me through the full story of request #15."
        return ResponseEntity.ok(
                auditLogService.getRequestAuditLogs(requestId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AuditLogResponseDto>> getUserAuditLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Helpful when reviewing a specific person's activity:
        // "What actions did Tariro perform this month?"
        return ResponseEntity.ok(
                auditLogService.getUserAuditLogs(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
        );
    }
}
