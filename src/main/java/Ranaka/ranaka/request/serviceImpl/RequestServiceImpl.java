package Ranaka.ranaka.request.serviceImpl;

import Ranaka.ranaka.audit.entity.AuditLog;
import Ranaka.ranaka.audit.repository.AuditLogRepository;
import Ranaka.ranaka.common.enums.*;
import Ranaka.ranaka.common.exception.ResourceNotFoundException;
import Ranaka.ranaka.department.entity.Department;
import Ranaka.ranaka.department.repository.DepartmentRepository;
import Ranaka.ranaka.notification.service.NotificationService;
import Ranaka.ranaka.request.dto.LineItemRequest;
import Ranaka.ranaka.request.dto.LineItemResponse;
import Ranaka.ranaka.request.dto.request.ApprovalActionRequest;
import Ranaka.ranaka.request.dto.request.CreateRequestDto;
import Ranaka.ranaka.request.dto.request.UpdateRequestDto;
import Ranaka.ranaka.request.dto.response.*;
import Ranaka.ranaka.request.entity.*;
import Ranaka.ranaka.request.repository.*;
import Ranaka.ranaka.request.service.RequestService;
import Ranaka.ranaka.user.domain.Role;
import Ranaka.ranaka.user.entity.User;
import Ranaka.ranaka.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comprehensive service implementation for managing procurement requests throughout their lifecycle.
 * This service handles the complete workflow from request creation to final authorization,
 * ensuring proper state transitions, notifications, audit logging, and scalability.
 *
 * Key Features:
 * - Role-based workflow management (REQUESTER -> ADMIN -> GM -> CEO)
 * - Comprehensive audit trail for all actions
 * - Automatic notifications and reminders
 * - Scalable data access with pagination
 * - Transactional integrity for all operations
 * - SLA tracking and overdue management
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RequestServiceImpl implements RequestService {

    private final ProcurementRequestRepository requestRepository;
    private final RequestApprovalRepository approvalRepository;
    private final RequestCommentRepository commentRepository;
    private final RequestAttachmentRepository attachmentRepository;
    private final RequestLineItemRepository lineItemRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    public RequestDetailResponseDto createRequest(CreateRequestDto request) {
        log.info("Creating new procurement request with title: {}", request.getTitle());

        User currentUser = getCurrentUser();
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        // A newly created request always starts as a draft.
        // Example: "Lindiwe is still checking supplier pricing, so she saves first and submits later."
        ProcurementRequest procurementRequest = ProcurementRequest.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .department(department)
                .justification(request.getJustification())
                .priority(request.getPriority())
                .requiredByDate(request.getRequiredByDate())
                .requester(currentUser)
                .status(RequestStatus.DRAFT)
                .currentStage(WorkflowStage.DRAFT)
                .returnCount(0)
                .isOverdue(false)
                .build();

        ProcurementRequest savedRequest = requestRepository.save(procurementRequest);

        // Add line items
        if (request.getLineItems() != null && !request.getLineItems().isEmpty()) {
            for (LineItemRequest lineItem : request.getLineItems()) {
                RequestLineItem item = RequestLineItem.builder()
                        .procurementRequest(savedRequest)
                        .itemDescription(lineItem.getItemDescription())
                        .quantity(lineItem.getQuantity())
                        .unitCost(lineItem.getUnitCost())
                        .unit(lineItem.getUnit() != null ? lineItem.getUnit() : "PCS")
                        .notes(lineItem.getNotes())
                        .build();
                item.calculateTotalCost();
                lineItemRepository.save(item);
                savedRequest.addLineItem(item);
            }
        }

        // Calculate total estimated cost
        savedRequest.calculateTotalEstimatedCost();
        ProcurementRequest finalRequest = requestRepository.save(savedRequest);
        int lineItemCount = request.getLineItems() != null ? request.getLineItems().size() : 0;

        // Log the creation action
        logAuditAction(finalRequest.getId(), "ProcurementRequest", AuditAction.CREATE_DRAFT,
                      "Request created in draft state with " + lineItemCount + " line item(s)", null, finalRequest.getTitle());

        log.info("Successfully created procurement request with ID: {}", finalRequest.getId());
        return mapToDetailResponse(finalRequest);
    }

    @Override
    public RequestDetailResponseDto updateRequest(Long requestId, UpdateRequestDto request) {
        log.info("Updating procurement request with ID: {}", requestId);

        ProcurementRequest procurementRequest = getRequestByIdAndValidateAccess(requestId);
        validateRequestIsDraft(procurementRequest);

        String oldTitle = procurementRequest.getTitle();

        // Update basic fields if provided
        if (request.getTitle() != null) procurementRequest.setTitle(request.getTitle());
        if (request.getDescription() != null) procurementRequest.setDescription(request.getDescription());
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findByIdAndDeletedAtIsNull(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
            procurementRequest.setDepartment(department);
        }
        if (request.getJustification() != null) procurementRequest.setJustification(request.getJustification());
        if (request.getPriority() != null) procurementRequest.setPriority(request.getPriority());
        if (request.getRequiredByDate() != null) procurementRequest.setRequiredByDate(request.getRequiredByDate());

        // Handle line items if provided
        if (request.getLineItems() != null && !request.getLineItems().isEmpty()) {
            // Replaced line items are archived so the audit trail keeps older draft values.
            lineItemRepository.softDeleteByProcurementRequestId(requestId, LocalDateTime.now());

            // Add new line items
            for (LineItemRequest lineItem : request.getLineItems()) {
                RequestLineItem item = RequestLineItem.builder()
                        .procurementRequest(procurementRequest)
                        .itemDescription(lineItem.getItemDescription())
                        .quantity(lineItem.getQuantity())
                        .unitCost(lineItem.getUnitCost())
                        .unit(lineItem.getUnit() != null ? lineItem.getUnit() : "PCS")
                        .notes(lineItem.getNotes())
                        .build();
                item.calculateTotalCost();
                lineItemRepository.save(item);
                procurementRequest.addLineItem(item);
            }
        }

        // Recalculate total estimated cost
        procurementRequest.calculateTotalEstimatedCost();

        ProcurementRequest updatedRequest = requestRepository.save(procurementRequest);

        // Log the update action
        logAuditAction(requestId, "ProcurementRequest", AuditAction.UPDATE_DRAFT,
                      "Request draft updated", oldTitle, updatedRequest.getTitle());

        log.info("Successfully updated procurement request with ID: {}", requestId);
        return mapToDetailResponse(updatedRequest);
    }

    /**
     * Retrieves detailed information about a specific request.
     * Access control ensures users can only view requests they have permission to see.
     *
     * @param requestId The ID of the request to retrieve
     * @return Detailed request information including history, comments, and attachments
     */
    @Override
    @Transactional(readOnly = true)
    public RequestDetailResponseDto getRequestById(Long requestId) {
        ProcurementRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        // Validate access permissions
        validateRequestAccess(request);

        return mapToDetailResponse(request);
    }

    /**
     * Retrieves paginated list of requests created by the current user.
     * This provides requesters with an overview of their request portfolio.
     *
     * @param pageable Pagination parameters
     * @return Paginated list of user's requests
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RequestListResponseDto> getMyRequests(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<ProcurementRequest> requests = requestRepository.findByRequesterId(currentUser.getId(), pageable);

        List<RequestListResponseDto> responseDtos = requests.getContent().stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responseDtos, pageable, requests.getTotalElements());
    }

    /**
     * Retrieves paginated list of all requests with optional filtering.
     * System admins and approvers can view requests based on their roles and permissions.
     *
     * @param filter Optional filtering criteria
     * @param pageable Pagination parameters
     * @return Paginated and filtered list of requests
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RequestListResponseDto> getAllRequests(RequestFilterDto filter, Pageable pageable) {
        User currentUser = getCurrentUser();
        List<ProcurementRequest> filteredRequests = requestRepository.findAll().stream()
                .filter(request -> canAccessRequestHistory(request, currentUser))
                .filter(request -> matchesFilter(request, filter))
                .sorted(Comparator.comparing(ProcurementRequest::getCreatedAt).reversed())
                .collect(Collectors.toList());

        return buildRequestPage(filteredRequests, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequestListResponseDto> getRequestsByStatus(String status, Pageable pageable) {
        RequestFilterDto filter = RequestFilterDto.builder()
                .status(RequestStatus.valueOf(status.toUpperCase()))
                .build();
        return getAllRequests(filter, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequestListResponseDto> getRequestsByPriority(String priority, Pageable pageable) {
        RequestFilterDto filter = RequestFilterDto.builder()
                .priority(RequestPriority.valueOf(priority.toUpperCase()))
                .build();
        return getAllRequests(filter, pageable);
    }

    /**
     * Submits a draft request for approval workflow.
     * This transitions the request from DRAFT to PENDING_ADMIN_RECOMMENDATION status.
     *
     * @param requestId The ID of the request to submit
     * @return Updated request details
     */
    @Override
    public RequestDetailResponseDto submitRequest(Long requestId) {
        log.info("Submitting procurement request with ID: {}", requestId);

        ProcurementRequest request = getRequestByIdAndValidateAccess(requestId);
        validateRequestIsDraft(request);
        validateRequestCompleteness(request);

        // This is the moment a request stops being a personal draft and starts becoming
        // shared workflow work for the Admin queue.
        // Update request status and stage
        request.setStatus(RequestStatus.PENDING_ADMIN_RECOMMENDATION);
        request.setCurrentStage(WorkflowStage.ADMIN_RECOMMENDATION);
        request.setSubmittedAt(LocalDateTime.now());
        request.setIsOverdue(false);
        request.setOverdueAt(null);

        ProcurementRequest submittedRequest = requestRepository.save(request);

        // Create notifications for admin users
        notifyAdminsOfNewRequest(submittedRequest);

        // Log the submission action
        logAuditAction(requestId, "ProcurementRequest", AuditAction.SUBMIT_REQUEST,
                      "Request submitted for approval", null, "PENDING_ADMIN_RECOMMENDATION");

        log.info("Successfully submitted procurement request with ID: {}", requestId);
        return mapToDetailResponse(submittedRequest);
    }

    /**
     * Admin recommends a request, moving it to GM approval stage.
     * Only admins can perform this action on requests in their stage.
     *
     * @param requestId The ID of the request to recommend
     * @param approvalRequest Approval details including comments
     * @return Updated request details
     */
    @Override
    public RequestDetailResponseDto recommendRequest(Long requestId, ApprovalActionRequest approvalRequest) {
        log.info("Admin recommending request with ID: {}", requestId);

        ProcurementRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        validateStageAction(request, WorkflowStage.ADMIN_RECOMMENDATION, "ADMIN");

        User currentUser = getCurrentUser();

        // Example: Tariro has checked budget alignment and is passing the request to Nyasha (GM).
        // Create approval history
        createApprovalHistory(request, WorkflowStage.ADMIN_RECOMMENDATION,
                            ApprovalAction.RECOMMEND, approvalRequest.getComment(), currentUser);

        // Update request status
        request.setStatus(RequestStatus.PENDING_GM_APPROVAL);
        request.setCurrentStage(WorkflowStage.GM_APPROVAL);
        request.setIsOverdue(false);
        request.setOverdueAt(null);

        ProcurementRequest updatedRequest = requestRepository.save(request);

        // Notify GM users
        notifyGmsOfPendingApproval(updatedRequest);

        // Log the recommendation action
        logAuditAction(requestId, "ProcurementRequest", AuditAction.RECOMMEND_REQUEST,
                      "Request recommended by admin", null, approvalRequest.getComment());

        log.info("Successfully recommended request with ID: {}", requestId);
        return mapToDetailResponse(updatedRequest);
    }

    /**
     * GM approves a request, moving it to CEO authorization stage.
     * Only GMs can perform this action on requests in their stage.
     *
     * @param requestId The ID of the request to approve
     * @param approvalRequest Approval details including comments
     * @return Updated request details
     */
    @Override
    public RequestDetailResponseDto approveRequest(Long requestId, ApprovalActionRequest approvalRequest) {
        log.info("GM approving request with ID: {}", requestId);

        ProcurementRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        validateStageAction(request, WorkflowStage.GM_APPROVAL, "GM");

        User currentUser = getCurrentUser();

        // Example: the GM is saying "This request is valid at management level;
        // it is ready for final executive authorization."
        // Create approval history
        createApprovalHistory(request, WorkflowStage.GM_APPROVAL,
                            ApprovalAction.APPROVE, approvalRequest.getComment(), currentUser);

        // Update request status
        request.setStatus(RequestStatus.PENDING_CEO_AUTHORIZATION);
        request.setCurrentStage(WorkflowStage.CEO_AUTHORIZATION);
        request.setIsOverdue(false);
        request.setOverdueAt(null);

        ProcurementRequest updatedRequest = requestRepository.save(request);

        // Notify CEO users
        notifyCeosOfPendingAuthorization(updatedRequest);

        // Log the approval action
        logAuditAction(requestId, "ProcurementRequest", AuditAction.APPROVE_REQUEST,
                      "Request approved by GM", null, approvalRequest.getComment());

        log.info("Successfully approved request with ID: {}", requestId);
        return mapToDetailResponse(updatedRequest);
    }

    /**
     * CEO authorizes a request, completing the approval workflow.
     * Only CEOs can perform this action on requests in their stage.
     *
     * @param requestId The ID of the request to authorize
     * @param approvalRequest Authorization details including comments
     * @return Updated request details
     */
    @Override
    public RequestDetailResponseDto authorizeRequest(Long requestId, ApprovalActionRequest approvalRequest) {
        log.info("CEO authorizing request with ID: {}", requestId);

        ProcurementRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        validateStageAction(request, WorkflowStage.CEO_AUTHORIZATION, "CEO");

        User currentUser = getCurrentUser();

        // Final step in the approval chain:
        // once this succeeds, the request is treated as completed for MVP purposes.
        // Create approval history
        createApprovalHistory(request, WorkflowStage.CEO_AUTHORIZATION,
                            ApprovalAction.AUTHORIZE, approvalRequest.getComment(), currentUser);

        // Complete the request
        request.setStatus(RequestStatus.COMPLETED);
        request.setCurrentStage(WorkflowStage.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        request.setIsOverdue(false);
        request.setOverdueAt(null);

        ProcurementRequest updatedRequest = requestRepository.save(request);

        // Notify requester of completion
        notifyRequesterOfCompletion(updatedRequest);

        // Log the authorization action
        logAuditAction(requestId, "ProcurementRequest", AuditAction.AUTHORIZE_REQUEST,
                      "Request authorized by CEO", null, approvalRequest.getComment());

        log.info("Successfully authorized request with ID: {}", requestId);
        return mapToDetailResponse(updatedRequest);
    }

    /**
     * Rejects a request at any approval stage, terminating the workflow.
     * Requires a comment explaining the rejection reason.
     *
     * @param requestId The ID of the request to reject
     * @param approvalRequest Rejection details including mandatory comment
     * @return Updated request details
     */
    @Override
    public RequestDetailResponseDto rejectRequest(Long requestId, ApprovalActionRequest approvalRequest) {
        log.info("Rejecting request with ID: {}", requestId);

        ProcurementRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        validateCanActOnRequest(request);

        if (approvalRequest.getComment() == null || approvalRequest.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("Comment is required when rejecting a request");
        }

        User currentUser = getCurrentUser();

        // Create approval history
        createApprovalHistory(request, request.getCurrentStage(),
                            ApprovalAction.REJECT, approvalRequest.getComment(), currentUser);

        // Update request status
        request.setStatus(RequestStatus.REJECTED);
        request.setCurrentStage(null); // Clear active stage
        request.setCompletedAt(LocalDateTime.now());
        request.setIsOverdue(false);
        request.setOverdueAt(null);

        ProcurementRequest updatedRequest = requestRepository.save(request);

        // Notify requester of rejection
        notifyRequesterOfRejection(updatedRequest, approvalRequest.getComment());

        // Log the rejection action
        logAuditAction(requestId, "ProcurementRequest", AuditAction.REJECT_REQUEST,
                      "Request rejected", null, approvalRequest.getComment());

        log.info("Successfully rejected request with ID: {}", requestId);
        return mapToDetailResponse(updatedRequest);
    }

    /**
     * Returns a request to the requester for corrections.
     * Increments return count and requires explanation comment.
     *
     * @param requestId The ID of the request to return
     * @param approvalRequest Return details including mandatory comment
     * @return Updated request details
     */
    @Override
    public RequestDetailResponseDto returnRequest(Long requestId, ApprovalActionRequest approvalRequest) {
        log.info("Returning request with ID: {} for correction", requestId);

        ProcurementRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        validateCanActOnRequest(request);

        if (approvalRequest.getComment() == null || approvalRequest.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("Comment is required when returning a request for correction");
        }

        User currentUser = getCurrentUser();

        // Returning is intentionally softer than rejecting:
        // the requester gets a path to fix the issue and try again.
        // Create approval history
        createApprovalHistory(request, request.getCurrentStage(),
                            ApprovalAction.RETURN_FOR_CORRECTION, approvalRequest.getComment(), currentUser);

        // Update request status
        request.setStatus(RequestStatus.RETURNED_FOR_CORRECTION);
        request.setCurrentStage(WorkflowStage.DRAFT); // Return to draft state
        request.setReturnCount(request.getReturnCount() + 1);
        request.setIsOverdue(false);
        request.setOverdueAt(null);

        ProcurementRequest updatedRequest = requestRepository.save(request);

        // Notify requester of return
        notifyRequesterOfReturn(updatedRequest, approvalRequest.getComment());

        // Log the return action
        logAuditAction(requestId, "ProcurementRequest", AuditAction.RETURN_REQUEST,
                      "Request returned for correction", null, approvalRequest.getComment());

        log.info("Successfully returned request with ID: {} for correction", requestId);
        return mapToDetailResponse(updatedRequest);
    }

    /**
     * Retrieves all requests currently pending admin recommendation.
     * Used by admin dashboard to show workload.
     *
     * @return List of requests pending admin action
     */
    @Override
    @Transactional(readOnly = true)
    public List<RequestListResponseDto> getPendingAdminRequests() {
        List<ProcurementRequest> requests = requestRepository.findByCurrentStage(WorkflowStage.ADMIN_RECOMMENDATION);
        return requests.stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all requests currently pending GM approval.
     * Used by GM dashboard to show workload.
     *
     * @return List of requests pending GM action
     */
    @Override
    @Transactional(readOnly = true)
    public List<RequestListResponseDto> getPendingGmRequests() {
        List<ProcurementRequest> requests = requestRepository.findByCurrentStage(WorkflowStage.GM_APPROVAL);
        return requests.stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all requests currently pending CEO authorization.
     * Used by CEO dashboard to show workload.
     *
     * @return List of requests pending CEO action
     */
    @Override
    @Transactional(readOnly = true)
    public List<RequestListResponseDto> getPendingCeoRequests() {
        List<ProcurementRequest> requests = requestRepository.findByCurrentStage(WorkflowStage.CEO_AUTHORIZATION);
        return requests.stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());
    }

    /**
     * Adds a comment to a request. Supports both public and internal comments.
     *
     * @param requestId The ID of the request to comment on
     * @param comment The comment text
     * @param isInternal Whether this is an internal comment (approvers only)
     */
    @Override
    public void addComment(Long requestId, String comment, boolean isInternal) {
        ProcurementRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        validateRequestAccess(request);
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment cannot be empty");
        }

        User currentUser = getCurrentUser();

        RequestComment requestComment = RequestComment.builder()
                .request(request)
                .commenter(currentUser)
                .comment(comment)
                .isInternal(isInternal)
                .build();

        commentRepository.save(requestComment);

        logAuditAction(requestId, "ProcurementRequest", AuditAction.CREATE_COMMENT,
                      "Comment added to request", null, isInternal ? "Internal comment" : "Public comment");
    }

    /**
     * Records an uploaded attachment for a request.
     *
     * @param requestId The ID of the request
     * @param fileName Original file name
     * @param filePath Stored file path
     * @param contentType File content type
     * @param fileSize File size in bytes
     */
    @Override
    public void uploadAttachment(Long requestId, String fileName, String filePath, String contentType, long fileSize) {
        ProcurementRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        validateRequestAccess(request);

        User currentUser = getCurrentUser();

        RequestAttachment attachment = RequestAttachment.builder()
                .request(request)
                .uploadedBy(currentUser)
                .fileName(fileName)
                .filePath(filePath)
                .contentType(contentType)
                .fileSize(fileSize)
                .build();

        attachmentRepository.save(attachment);

        logAuditAction(requestId, "ProcurementRequest", AuditAction.UPLOAD_ATTACHMENT,
                      "Attachment uploaded", null, fileName);
    }

    /**
     * Retrieves all comments for a request, respecting visibility rules.
     *
     * @param requestId The ID of the request
     * @return List of comments visible to current user
     */
    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getRequestComments(Long requestId) {
        ProcurementRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        validateRequestAccess(request);

        List<RequestComment> comments = commentRepository.findByRequestIdOrderByCreatedAtDesc(requestId);

        return comments.stream()
                .filter(comment -> canViewComment(comment))
                .map(this::mapToCommentDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all attachments for a request.
     *
     * @param requestId The ID of the request
     * @return List of attachments
     */
    @Override
    @Transactional(readOnly = true)
    public List<AttachmentDto> getRequestAttachments(Long requestId) {
        ProcurementRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        validateRequestAccess(request);

        List<RequestAttachment> attachments = attachmentRepository.findByRequestIdOrderByUploadedAtDesc(requestId);

        return attachments.stream()
                .map(this::mapToAttachmentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDownloadDto getRequestAttachmentDownload(Long requestId, Long attachmentId) {
        ProcurementRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        validateRequestAccess(request);

        RequestAttachment attachment = attachmentRepository.findByIdAndRequestId(attachmentId, requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        return AttachmentDownloadDto.builder()
                .id(attachment.getId())
                .requestId(requestId)
                .fileName(attachment.getFileName())
                .filePath(attachment.getFilePath())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .build();
    }

    /**
     * Retrieves all overdue requests across the system.
     * Used for dashboards and escalation processes.
     *
     * @return List of overdue requests
     */
    @Override
    @Transactional(readOnly = true)
    public List<RequestListResponseDto> getOverdueRequests() {
        User currentUser = getCurrentUser();
        List<ProcurementRequest> requests = requestRepository.findByIsOverdueTrue().stream()
                .filter(request -> canViewOverdueRequest(currentUser, request))
                .collect(Collectors.toList());

        return requests.stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());
    }

    /**
     * Counts requests by status for dashboard metrics.
     *
     * @param status The status to count
     * @return Number of requests with the given status
     */
    @Override
    @Transactional(readOnly = true)
    public long countRequestsByStatus(String status) {
        try {
            RequestStatus requestStatus = RequestStatus.valueOf(status.toUpperCase());
            if (requestStatus == RequestStatus.COMPLETED) {
                return requestRepository.findAll().stream()
                        .filter(request -> request.getStatus() == RequestStatus.COMPLETED
                                || request.getStatus() == RequestStatus.AUTHORIZED)
                        .count();
            }
            return requestRepository.findByStatus(requestStatus).size();
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    /**
     * Validates if the current user can perform a specific action on a request.
     * This is a critical security method that enforces workflow rules.
     *
     * @param requestId The ID of the request
     * @param action The action to validate
     * @return true if the user can perform the action
     */
    @Override
    @Transactional(readOnly = true)
    public boolean canUserActOnRequest(Long requestId, String action) {
        try {
            ProcurementRequest request = requestRepository.findById(requestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

            User currentUser = getCurrentUser();
            String userRole = currentUser.getRole().name();
            String normalizedAction = action == null ? "" : action.toUpperCase();

            // Requester can only act on their own draft/returned requests
            if (request.getRequester().getId().equals(currentUser.getId())) {
                boolean editable = request.getStatus() == RequestStatus.DRAFT ||
                        request.getStatus() == RequestStatus.RETURNED_FOR_CORRECTION;
                return switch (normalizedAction) {
                    case "VIEW" -> true;
                    case "UPDATE", "SUBMIT", "COMMENT", "ATTACH" -> editable;
                    default -> false;
                };
            }

            // Approvers can only act on requests in their stage
            switch (normalizedAction) {
                case "RECOMMEND":
                    return userRole.equals("ADMIN") &&
                           request.getCurrentStage() == WorkflowStage.ADMIN_RECOMMENDATION;
                case "APPROVE":
                    return userRole.equals("GM") &&
                           request.getCurrentStage() == WorkflowStage.GM_APPROVAL;
                case "AUTHORIZE":
                    return userRole.equals("CEO") &&
                           request.getCurrentStage() == WorkflowStage.CEO_AUTHORIZATION;
                case "REJECT":
                case "RETURN":
                    return canActOnCurrentStage(request, userRole);
                case "VIEW":
                case "COMMENT":
                case "ATTACH":
                    return canAccessRequestHistory(request, currentUser);
                default:
                    return false;
            }
        } catch (Exception e) {
            log.error("Error validating user action on request {}: {}", requestId, e.getMessage());
            return false;
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Retrieves the currently authenticated user from security context.
     * This is used throughout the service for audit logging and access control.
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }

        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        }

        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Authenticated user email is not available");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private boolean canViewOverdueRequest(User currentUser, ProcurementRequest request) {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }

        return switch (currentUser.getRole()) {
            case SYSTEM_ADMIN -> true;
            case ADMIN -> request.getCurrentStage() == WorkflowStage.ADMIN_RECOMMENDATION;
            case GM -> request.getCurrentStage() == WorkflowStage.GM_APPROVAL;
            case CEO -> request.getCurrentStage() == WorkflowStage.CEO_AUTHORIZATION;
            default -> false;
        };
    }

    /**
     * Retrieves a request by ID and validates that the current user has access to it.
     */
    private ProcurementRequest getRequestByIdAndValidateAccess(Long requestId) {
        ProcurementRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));
        validateRequestAccess(request);
        return request;
    }

    /**
     * Validates that a request is in draft state for modification.
     */
    private void validateRequestIsDraft(ProcurementRequest request) {
        if (request.getStatus() != RequestStatus.DRAFT &&
                request.getStatus() != RequestStatus.RETURNED_FOR_CORRECTION) {
            throw new IllegalStateException("Request can only be modified while in draft or returned state");
        }
    }

    /**
     * Validates that a request has all required fields before submission.
     */
    private void validateRequestCompleteness(ProcurementRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Request title is required");
        }
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Request description is required");
        }
        if (request.getJustification() == null || request.getJustification().trim().isEmpty()) {
            throw new IllegalArgumentException("Request justification is required");
        }
        if (request.getPriority() == null) {
            throw new IllegalArgumentException("Request priority is required");
        }
        if (request.getRequiredByDate() == null) {
            throw new IllegalArgumentException("Required by date is required");
        }
        if (request.getRequiredByDate().isBefore(LocalDateTime.now().toLocalDate())) {
            throw new IllegalArgumentException("Required by date cannot be in the past");
        }
        if (request.getLineItems() == null || request.getLineItems().isEmpty()) {
            throw new IllegalArgumentException("At least one line item is required");
        }
    }

    /**
     * Validates that the current user has permission to access a request.
     */
    private void validateRequestAccess(ProcurementRequest request) {
        User currentUser = getCurrentUser();

        // Requesters can access their own requests
        if (request.getRequester().getId().equals(currentUser.getId())) {
            return;
        }

        // System admins can access all requests
        if (currentUser.getRole() == Role.SYSTEM_ADMIN) {
            return;
        }

        if (canAccessRequestHistory(request, currentUser)) {
            return;
        }

        throw new IllegalArgumentException("Access denied to this request");
    }

    /**
     * Validates that a user can act on a request in its current stage.
     */
    private void validateStageAction(ProcurementRequest request, WorkflowStage requiredStage, String requiredRole) {
        if (request.getCurrentStage() != requiredStage) {
            throw new IllegalStateException("Request is not in the correct stage for this action");
        }

        User currentUser = getCurrentUser();
        if (!currentUser.getRole().name().equals(requiredRole)) {
            throw new IllegalArgumentException("User does not have permission to perform this action");
        }
    }

    /**
     * Validates that the current user can act on a request in any approval stage.
     */
    private void validateCanActOnRequest(ProcurementRequest request) {
        User currentUser = getCurrentUser();
        String userRole = currentUser.getRole().name();

        if (!canActOnCurrentStage(request, userRole)) {
            throw new IllegalArgumentException("User does not have permission to act on this request");
        }
    }

    /**
     * Checks if a user role can act on a request in its current stage.
     */
    private boolean canActOnCurrentStage(ProcurementRequest request, String userRole) {
        if (request.getCurrentStage() == null) return false;

        switch (request.getCurrentStage()) {
            case ADMIN_RECOMMENDATION:
                return userRole.equals("ADMIN");
            case GM_APPROVAL:
                return userRole.equals("GM");
            case CEO_AUTHORIZATION:
                return userRole.equals("CEO");
            default:
                return false;
        }
    }

    private boolean canAccessRequestHistory(ProcurementRequest request, User currentUser) {
        if (currentUser.getRole() == Role.SYSTEM_ADMIN) {
            return true;
        }

        if (request.getRequester().getId().equals(currentUser.getId())) {
            return true;
        }

        if (isApproverRole(currentUser.getRole())) {
            // Approvers are allowed to see non-draft history even after the request leaves their queue.
            // Example: a GM should still be able to open a request they approved last week.
            return request.getStatus() != RequestStatus.DRAFT
                    || (request.getReturnCount() != null && request.getReturnCount() > 0);
        }

        return false;
    }

    private boolean isApproverRole(Role role) {
        return role == Role.ADMIN || role == Role.GM || role == Role.CEO;
    }

    private boolean matchesFilter(ProcurementRequest request, RequestFilterDto filter) {
        if (filter == null) {
            return true;
        }
        if (filter.getStatus() != null && request.getStatus() != filter.getStatus()) {
            return false;
        }
        if (filter.getPriority() != null && request.getPriority() != filter.getPriority()) {
            return false;
        }
        if (filter.getDepartmentId() != null && !request.getDepartment().getId().equals(filter.getDepartmentId())) {
            return false;
        }
        if (filter.getRequesterId() != null && !request.getRequester().getId().equals(filter.getRequesterId())) {
            return false;
        }
        if (filter.getIsOverdue() != null && !request.getIsOverdue().equals(filter.getIsOverdue())) {
            return false;
        }
        if (filter.getStartDate() != null && request.getCreatedAt().toLocalDate().isBefore(filter.getStartDate())) {
            return false;
        }
        if (filter.getEndDate() != null && request.getCreatedAt().toLocalDate().isAfter(filter.getEndDate())) {
            return false;
        }
        if (filter.getSearchTerm() != null && !filter.getSearchTerm().isBlank()) {
            String searchTerm = filter.getSearchTerm().toLowerCase();
            return request.getTitle().toLowerCase().contains(searchTerm)
                    || request.getDescription().toLowerCase().contains(searchTerm)
                    || request.getJustification().toLowerCase().contains(searchTerm);
        }

        return true;
    }

    private Page<RequestListResponseDto> buildRequestPage(List<ProcurementRequest> requests, Pageable pageable) {
        List<RequestListResponseDto> responseDtos = requests.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .map(this::mapToListResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(responseDtos, pageable, requests.size());
    }

    /**
     * Checks if the current user can view a comment (internal comments are approver-only).
     */
    private boolean canViewComment(RequestComment comment) {
        if (!comment.isInternal()) return true;

        User currentUser = getCurrentUser();
        String userRole = currentUser.getRole().name();

        return userRole.equals("ADMIN") || userRole.equals("GM") ||
               userRole.equals("CEO") || userRole.equals("SYSTEM_ADMIN");
    }

    /**
     * Creates an approval history record for audit purposes.
     */
    private void createApprovalHistory(ProcurementRequest request, WorkflowStage stage,
                                     ApprovalAction action, String comment, User approver) {
        RequestApproval approval = RequestApproval.builder()
                .request(request)
                .approver(approver)
                .stage(stage)
                .action(action)
                .comment(comment)
                .actionDate(LocalDateTime.now())
                .build();

        approvalRepository.save(approval);
    }

    /**
     * Logs an audit action for compliance and tracking.
     */
    private void logAuditAction(Long entityId, String entityType, AuditAction action,
                              String description, String oldValue, String newValue) {
        try {
            User currentUser = getCurrentUser();

            AuditLog auditLog = AuditLog.builder()
                    .user(currentUser)
                    .action(action)
                    .description(description)
                    .entityId(entityId)
                    .entityType(entityType)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .ipAddress("system") // In real implementation, get from request
                    .userAgent("system")
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log audit action: {}", e.getMessage());
            // Don't fail the main operation if audit logging fails
        }
    }

    // ==================== NOTIFICATION METHODS ====================

    /**
     * Notifies all ADMIN users about a new request that requires their recommendation.
     * This sends both in-app notifications and email notifications to all users with ADMIN role.
     * Each admin receives a targeted notification allowing them to view and act on the request.
     * This ensures that multiple admins can review and the first one to act will advance the workflow.
     */
    private void notifyAdminsOfNewRequest(ProcurementRequest request) {
        try {
            // Fetch all users with ADMIN role to send targeted notifications
            List<User> adminUsers = userRepository.findByRole(Role.ADMIN);
            log.info("Notifying {} admin users about new request: {}", adminUsers.size(), request.getTitle());

            // Send individual notifications and emails to each admin
            for (User admin : adminUsers) {
                notificationService.createNotification(admin, NotificationType.REQUEST_SUBMITTED,
                                  "New Request Submitted",
                                  String.format("A new procurement request '%s' from %s (Department: %s) has been submitted and requires your recommendation.",
                                               request.getTitle(),
                                               request.getRequester().getFirstName() + " " + request.getRequester().getLastName(),
                                               request.getDepartment().getName()),
                                  request.getId(), "ProcurementRequest");
            }
        } catch (Exception e) {
            log.error("Failed to notify admins about new request: {}", e.getMessage());
        }
    }

    /**
     * Notifies all GM users about a request pending approval.
     * This sends both in-app notifications and email notifications to all users with GM role.
     * Each GM receives a targeted notification with request details to enable informed decision-making.
     * Multiple GMs are notified to ensure proper distribution of workload and timely approvals.
     */
    private void notifyGmsOfPendingApproval(ProcurementRequest request) {
        try {
            // Fetch all users with GM role to send targeted notifications
            List<User> gmUsers = userRepository.findByRole(Role.GM);
            log.info("Notifying {} GM users about pending approval: {}", gmUsers.size(), request.getTitle());

            // Send individual notifications and emails to each GM
            for (User gm : gmUsers) {
                notificationService.createNotification(gm, NotificationType.REQUEST_MOVED_TO_NEXT_STAGE,
                                  "Request Ready for Approval",
                                  String.format("Procurement request '%s' has been recommended by admin and requires your approval. " +
                                               "Priority: %s | Estimated Cost: %s",
                                               request.getTitle(),
                                               request.getPriority().name(),
                                               String.format("%.2f", request.getEstimatedCost())),
                                  request.getId(), "ProcurementRequest");
            }
        } catch (Exception e) {
            log.error("Failed to notify GMs about pending approval: {}", e.getMessage());
        }
    }

    /**
     * Notifies all CEO users about a request pending authorization.
     * This sends both in-app notifications and email notifications to all users with CEO role.
     * Each CEO receives a targeted notification with complete request context for final authorization decision.
     * CEOs are kept informed of high-value procurement decisions requiring their executive approval.
     */
    private void notifyCeosOfPendingAuthorization(ProcurementRequest request) {
        try {
            // Fetch all users with CEO role to send targeted notifications
            List<User> ceoUsers = userRepository.findByRole(Role.CEO);
            log.info("Notifying {} CEO users about pending authorization: {}", ceoUsers.size(), request.getTitle());

            // Send individual notifications and emails to each CEO
            for (User ceo : ceoUsers) {
                notificationService.createNotification(ceo, NotificationType.REQUEST_MOVED_TO_NEXT_STAGE,
                                  "Request Ready for Authorization",
                                  String.format("Procurement request '%s' has been approved by GM and requires your authorization. " +
                                               "Priority: %s | Estimated Cost: %s | Department: %s",
                                               request.getTitle(),
                                               request.getPriority().name(),
                                               String.format("%.2f", request.getEstimatedCost()),
                                               request.getDepartment().getName()),
                                  request.getId(), "ProcurementRequest");
            }
        } catch (Exception e) {
            log.error("Failed to notify CEOs about pending authorization: {}", e.getMessage());
        }
    }

    /**
     * Notifies the requester that their request has been completed.
     */
    private void notifyRequesterOfCompletion(ProcurementRequest request) {
        // Human example:
        // "Lindiwe, your printer request is fully approved. You do not need to keep chasing approvers."
        notificationService.createNotification(request.getRequester(), NotificationType.REQUEST_COMPLETED,
                          "Request Completed",
                          String.format("Your procurement request '%s' has been authorized and completed.",
                                       request.getTitle()),
                          request.getId(), "ProcurementRequest");
    }

    /**
     * Notifies the requester that their request has been rejected.
     */
    private void notifyRequesterOfRejection(ProcurementRequest request, String reason) {
        notificationService.createNotification(request.getRequester(), NotificationType.REQUEST_REJECTED,
                          "Request Rejected",
                          String.format("Your procurement request '%s' has been rejected. Reason: %s",
                                       request.getTitle(), reason),
                          request.getId(), "ProcurementRequest");
    }

    /**
     * Notifies the requester that their request has been returned for correction.
     */
    private void notifyRequesterOfReturn(ProcurementRequest request, String reason) {
        notificationService.createNotification(request.getRequester(), NotificationType.REQUEST_RETURNED,
                          "Request Returned for Correction",
                          String.format("Your procurement request '%s' has been returned for correction. Reason: %s",
                                       request.getTitle(), reason),
                          request.getId(), "ProcurementRequest");
    }

    // ==================== MAPPING METHODS ====================

    /**
     * Maps a ProcurementRequest entity to a detailed response DTO.
     */
    private RequestDetailResponseDto mapToDetailResponse(ProcurementRequest request) {
        List<ApprovalHistoryDto> approvalHistory = approvalRepository
                .findByRequestIdOrderByCreatedAtDesc(request.getId())
                .stream()
                .map(this::mapToApprovalHistoryDto)
                .collect(Collectors.toList());

        List<CommentDto> comments = getRequestComments(request.getId());
        List<AttachmentDto> attachments = getRequestAttachments(request.getId());

        // Map line items
        List<LineItemResponse> lineItems = request.getLineItems().stream()
                .filter(RequestLineItem::isActive)
                .map(item -> LineItemResponse.builder()
                        .id(item.getId())
                        .itemDescription(item.getItemDescription())
                        .quantity(item.getQuantity())
                        .unitCost(item.getUnitCost())
                        .totalCost(item.getTotalCost())
                        .unit(item.getUnit())
                        .notes(item.getNotes())
                        .createdAt(item.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return RequestDetailResponseDto.builder()
                .id(request.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .lineItems(lineItems)
                .estimatedCost(request.getEstimatedCost())
                .departmentName(request.getDepartment().getName())
                .departmentId(request.getDepartment().getId())
                .justification(request.getJustification())
                .priority(request.getPriority())
                .requiredByDate(request.getRequiredByDate())
                .status(request.getStatus())
                .currentStage(request.getCurrentStage())
                .submittedAt(request.getSubmittedAt())
                .completedAt(request.getCompletedAt())
                .isOverdue(request.getIsOverdue())
                .overdueAt(request.getOverdueAt())
                .returnCount(request.getReturnCount())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .requesterId(request.getRequester().getId())
                .requesterName(request.getRequester().getFirstName() + " " + request.getRequester().getLastName())
                .requesterEmail(request.getRequester().getEmail())
                .approvalHistory(approvalHistory)
                .comments(comments)
                .attachments(attachments)
                .build();
    }

    /**
     * Maps a ProcurementRequest entity to a list response DTO.
     */
    private RequestListResponseDto mapToListResponse(ProcurementRequest request) {
        return RequestListResponseDto.builder()
                .id(request.getId())
                .title(request.getTitle())
                .status(request.getStatus())
                .currentStage(request.getCurrentStage())
                .priority(request.getPriority())
                .estimatedCost(request.getEstimatedCost())
                .requiredByDate(request.getRequiredByDate())
                .requesterName(request.getRequester().getFirstName() + " " + request.getRequester().getLastName())
                .departmentName(request.getDepartment().getName())
                .submittedAt(request.getSubmittedAt())
                .createdAt(request.getCreatedAt())
                .isOverdue(request.getIsOverdue())
                .returnCount(request.getReturnCount())
                .build();
    }

    /**
     * Maps a RequestApproval entity to an approval history DTO.
     */
    private ApprovalHistoryDto mapToApprovalHistoryDto(RequestApproval approval) {
        return ApprovalHistoryDto.builder()
                .id(approval.getId())
                .stage(approval.getStage())
                .action(approval.getAction())
                .comment(approval.getComment())
                .actionDate(approval.getActionDate())
                .approverName(approval.getApprover().getFirstName() + " " + approval.getApprover().getLastName())
                .approverRole(approval.getApprover().getRole().name())
                .build();
    }

    /**
     * Maps a RequestComment entity to a comment DTO.
     */
    private CommentDto mapToCommentDto(RequestComment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .comment(comment.getComment())
                .isInternal(comment.isInternal())
                .createdAt(comment.getCreatedAt())
                .commenterName(comment.getCommenter().getFirstName() + " " + comment.getCommenter().getLastName())
                .commenterRole(comment.getCommenter().getRole().name())
                .build();
    }

    /**
     * Maps a RequestAttachment entity to an attachment DTO.
     */
    private AttachmentDto mapToAttachmentDto(RequestAttachment attachment) {
        String downloadPath = "/api/v1/requests/" + attachment.getRequest().getId()
                + "/attachments/" + attachment.getId() + "/download";

        return AttachmentDto.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .uploadedAt(attachment.getUploadedAt())
                .uploadedByName(attachment.getUploadedBy().getFirstName() + " " + attachment.getUploadedBy().getLastName())
                .downloadUrl(downloadPath)
                .fileUrl(downloadPath)
                .build();
    }
}
