package Ranaka.ranaka.request.controller;

import Ranaka.ranaka.request.dto.request.ApprovalActionRequest;
import Ranaka.ranaka.request.dto.request.CreateRequestDto;
import Ranaka.ranaka.request.dto.request.UpdateRequestDto;
import Ranaka.ranaka.request.dto.response.*;
import Ranaka.ranaka.request.service.AttachmentStorageService;
import Ranaka.ranaka.request.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * REST Controller for Procurement Request Management.
 * Provides comprehensive endpoints for the complete procurement workflow lifecycle.
 *
 * Endpoints are secured with role-based access control ensuring users can only
 * perform actions appropriate to their role in the approval process.
 */
@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;
    private final AttachmentStorageService attachmentStorageService;

    /**
     * Creates a new procurement request in draft state.
     * Only authenticated users can create requests.
     *
     * @param request The request creation data
     * @return Detailed response of the created request
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RequestDetailResponseDto> createRequest(@Valid @RequestBody CreateRequestDto request) {
        RequestDetailResponseDto response = requestService.createRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing draft request.
     * Only the requester can update their own draft requests.
     *
     * @param requestId The ID of the request to update
     * @param request Updated request data
     * @return Detailed response of the updated request
     */
    @PutMapping("/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RequestDetailResponseDto> updateRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody UpdateRequestDto request) {
        RequestDetailResponseDto response = requestService.updateRequest(requestId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Submits a draft request for approval workflow.
     * Transitions request from DRAFT to PENDING_ADMIN_RECOMMENDATION.
     *
     * @param requestId The ID of the request to submit
     * @return Updated request details
     */
    @PostMapping("/{requestId}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RequestDetailResponseDto> submitRequest(@PathVariable Long requestId) {
        RequestDetailResponseDto response = requestService.submitRequest(requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves detailed information about a specific request.
     * Access control ensures users can only view requests they have permission to access.
     *
     * @param requestId The ID of the request to retrieve
     * @return Detailed request information including history and attachments
     */
    @GetMapping("/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RequestDetailResponseDto> getRequestById(@PathVariable Long requestId) {
        RequestDetailResponseDto response = requestService.getRequestById(requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves paginated list of requests created by the current user.
     * Provides requesters with an overview of their request portfolio.
     *
     * @param page Page number (0-based)
     * @param size Page size
     * @param sort Sort field
     * @param direction Sort direction
     * @return Paginated list of user's requests
     */
    @GetMapping("/my-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<RequestListResponseDto>> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<RequestListResponseDto> response = requestService.getMyRequests(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves paginated list of all requests with optional filtering.
     * System admins and approvers can view requests based on their permissions.
     *
     * @param filter Optional filtering criteria
     * @param page Page number (0-based)
     * @param size Page size
     * @return Paginated and filtered list of requests
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GM', 'CEO', 'SYSTEM_ADMIN')")
    public ResponseEntity<Page<RequestListResponseDto>> getAllRequests(
            RequestFilterDto filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequestListResponseDto> response = requestService.getAllRequests(filter, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GM', 'CEO', 'SYSTEM_ADMIN')")
    public ResponseEntity<Page<RequestListResponseDto>> getRequestsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(requestService.getRequestsByStatus(status, pageable));
    }

    @GetMapping("/priority/{priority}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GM', 'CEO', 'SYSTEM_ADMIN')")
    public ResponseEntity<Page<RequestListResponseDto>> getRequestsByPriority(
            @PathVariable String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(requestService.getRequestsByPriority(priority, pageable));
    }

    /**
     * Retrieves all requests currently pending admin recommendation.
     * Used by admin dashboard to display workload.
     *
     * @return List of requests pending admin action
     */
    @GetMapping("/pending/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RequestListResponseDto>> getPendingAdminRequests() {
        List<RequestListResponseDto> response = requestService.getPendingAdminRequests();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all requests currently pending GM approval.
     * Used by GM dashboard to display workload.
     *
     * @return List of requests pending GM action
     */
    @GetMapping("/pending/gm")
    @PreAuthorize("hasRole('GM')")
    public ResponseEntity<List<RequestListResponseDto>> getPendingGmRequests() {
        List<RequestListResponseDto> response = requestService.getPendingGmRequests();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all requests currently pending CEO authorization.
     * Used by CEO dashboard to display workload.
     *
     * @return List of requests pending CEO action
     */
    @GetMapping("/pending/ceo")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<List<RequestListResponseDto>> getPendingCeoRequests() {
        List<RequestListResponseDto> response = requestService.getPendingCeoRequests();
        return ResponseEntity.ok(response);
    }

    /**
     * Admin recommends a request, moving it to GM approval stage.
     * Only admins can perform this action on requests in their stage.
     *
     * @param requestId The ID of the request to recommend
     * @param request Approval details including comments
     * @return Updated request details
     */
    @PostMapping("/{requestId}/recommend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RequestDetailResponseDto> recommendRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalActionRequest request
    ) {
        RequestDetailResponseDto response = requestService.recommendRequest(requestId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GM approves a request, moving it to CEO authorization stage.
     * Only GMs can perform this action on requests in their stage.
     *
     * @param requestId The ID of the request to approve
     * @param request Approval details including comments
     * @return Updated request details
     */
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('GM')")
    public ResponseEntity<RequestDetailResponseDto> approveRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalActionRequest request) {
        RequestDetailResponseDto response = requestService.approveRequest(requestId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * CEO authorizes a request, completing the approval workflow.
     * Only CEOs can perform this action on requests in their stage.
     *
     * @param requestId The ID of the request to authorize
     * @param request Authorization details including comments
     * @return Updated request details
     */
    @PostMapping("/{requestId}/authorize")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<RequestDetailResponseDto> authorizeRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalActionRequest request) {
        RequestDetailResponseDto response = requestService.authorizeRequest(requestId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Rejects a request at any approval stage, terminating the workflow.
     * Requires a comment explaining the rejection reason.
     *
     * @param requestId The ID of the request to reject
     * @param request Rejection details including mandatory comment
     * @return Updated request details
     */
    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'GM', 'CEO')")
    public ResponseEntity<RequestDetailResponseDto> rejectRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalActionRequest request) {
        RequestDetailResponseDto response = requestService.rejectRequest(requestId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns a request to the requester for corrections.
     * Increments return count and requires explanation comment.
     *
     * @param requestId The ID of the request to return
     * @param request Return details including mandatory comment
     * @return Updated request details
     */
    @PostMapping("/{requestId}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'GM', 'CEO')")
    public ResponseEntity<RequestDetailResponseDto> returnRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ApprovalActionRequest request) {
        RequestDetailResponseDto response = requestService.returnRequest(requestId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Adds a comment to a request. Supports both public and internal comments.
     *
     * @param requestId The ID of the request to comment on
     * @param comment The comment text
     * @param isInternal Whether this is an internal comment (approvers only)
     * @return Success response
     */
    @PostMapping("/{requestId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> addComment(
            @PathVariable Long requestId,
            @RequestParam String comment,
            @RequestParam(defaultValue = "false") boolean isInternal) {
        requestService.addComment(requestId, comment, isInternal);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Retrieves all comments for a request, respecting visibility rules.
     *
     * @param requestId The ID of the request
     * @return List of comments visible to current user
     */
    @GetMapping("/{requestId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommentDto>> getRequestComments(@PathVariable Long requestId) {
        List<CommentDto> response = requestService.getRequestComments(requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Uploads an attachment to a request.
     * Validates file type and size before storing.
     *
     * @param requestId The ID of the request
     * @param file The file to upload
     * @return Success response
     */
    @PostMapping("/{requestId}/attachments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> uploadAttachment(
            @PathVariable Long requestId,
            @RequestParam("file") MultipartFile file) {

        // Basic file validation
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String storedFilePath = attachmentStorageService.store(file);
            requestService.uploadAttachment(requestId, file.getOriginalFilename(),
                    storedFilePath, file.getContentType(), file.getSize());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store this attachment.");
        }

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Retrieves all attachments for a request.
     *
     * @param requestId The ID of the request
     * @return List of attachments
     */
    @GetMapping("/{requestId}/attachments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AttachmentDto>> getRequestAttachments(@PathVariable Long requestId) {
        List<AttachmentDto> response = requestService.getRequestAttachments(requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{requestId}/attachments/{attachmentId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadRequestAttachment(
            @PathVariable Long requestId,
            @PathVariable Long attachmentId) {
        AttachmentDownloadDto attachment = requestService.getRequestAttachmentDownload(requestId, attachmentId);
        Resource resource = attachmentStorageService.loadAsResource(attachment.getFilePath());

        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment file not found.");
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (attachment.getContentType() != null && !attachment.getContentType().isBlank()) {
            mediaType = MediaType.parseMediaType(attachment.getContentType());
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(attachment.getFileSize() == null ? 0 : attachment.getFileSize())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(attachment.getFileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(resource);
    }

    /**
     * Retrieves all overdue requests across the system.
     * Used for dashboards and escalation processes.
     *
     * @return List of overdue requests
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'GM', 'CEO', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<RequestListResponseDto>> getOverdueRequests() {
        List<RequestListResponseDto> response = requestService.getOverdueRequests();
        return ResponseEntity.ok(response);
    }

    /**
     * Validates if the current user can perform a specific action on a request.
     * This endpoint is used by the frontend to enable/disable UI elements.
     *
     * @param requestId The ID of the request
     * @param action The action to validate
     * @return true if the user can perform the action
     */
    @GetMapping("/{requestId}/can-act")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> canUserActOnRequest(
            @PathVariable Long requestId,
            @RequestParam String action) {
        boolean canAct = requestService.canUserActOnRequest(requestId, action);
        return ResponseEntity.ok(canAct);
    }
}
