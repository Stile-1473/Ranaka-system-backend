package Ranaka.ranaka.request.controller;

import Ranaka.ranaka.request.dto.request.ApprovalActionRequest;
import Ranaka.ranaka.request.dto.response.RequestDetailResponseDto;
import Ranaka.ranaka.request.dto.response.RequestListResponseDto;
import Ranaka.ranaka.request.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final RequestService requestService;

    @GetMapping("/pending/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RequestListResponseDto>> getPendingAdminRequests() {
        return ResponseEntity.ok(requestService.getPendingAdminRequests());
    }

    @GetMapping("/pending/gm")
    @PreAuthorize("hasRole('GM')")
    public ResponseEntity<List<RequestListResponseDto>> getPendingGmRequests() {
        return ResponseEntity.ok(requestService.getPendingGmRequests());
    }

    @GetMapping("/pending/ceo")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<List<RequestListResponseDto>> getPendingCeoRequests() {
        return ResponseEntity.ok(requestService.getPendingCeoRequests());
    }

    @PostMapping("/{requestId}/recommend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RequestDetailResponseDto> recommendRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalActionRequest request) {
        return ResponseEntity.ok(requestService.recommendRequest(requestId, request));
    }

    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('GM')")
    public ResponseEntity<RequestDetailResponseDto> approveRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalActionRequest request) {
        return ResponseEntity.ok(requestService.approveRequest(requestId, request));
    }

    @PostMapping("/{requestId}/authorize")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<RequestDetailResponseDto> authorizeRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalActionRequest request) {
        return ResponseEntity.ok(requestService.authorizeRequest(requestId, request));
    }

    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'GM', 'CEO')")
    public ResponseEntity<RequestDetailResponseDto> rejectRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalActionRequest request) {
        return ResponseEntity.ok(requestService.rejectRequest(requestId, request));
    }

    @PostMapping("/{requestId}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'GM', 'CEO')")
    public ResponseEntity<RequestDetailResponseDto> returnRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalActionRequest request) {
        return ResponseEntity.ok(requestService.returnRequest(requestId, request));
    }
}
