package Ranaka.ranaka.request.service;

import Ranaka.ranaka.request.dto.request.ApprovalActionRequest;
import Ranaka.ranaka.request.dto.request.CreateRequestDto;
import Ranaka.ranaka.request.dto.request.UpdateRequestDto;
import Ranaka.ranaka.request.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RequestService {

    // CRUD Operations

    RequestDetailResponseDto createRequest(CreateRequestDto request);

    RequestDetailResponseDto updateRequest(Long requestId, UpdateRequestDto request);

    RequestDetailResponseDto getRequestById(Long requestId);

    Page<RequestListResponseDto> getMyRequests(Pageable pageable);

    Page<RequestListResponseDto> getAllRequests(RequestFilterDto filter, Pageable pageable);

    Page<RequestListResponseDto> getRequestsByStatus(String status, Pageable pageable);

    Page<RequestListResponseDto> getRequestsByPriority(String priority, Pageable pageable);


    // Workflow Operations
    RequestDetailResponseDto submitRequest(Long requestId);

    RequestDetailResponseDto recommendRequest(Long requestId, ApprovalActionRequest request);

    RequestDetailResponseDto approveRequest(Long requestId, ApprovalActionRequest request);

    RequestDetailResponseDto authorizeRequest(Long requestId, ApprovalActionRequest request);

    RequestDetailResponseDto rejectRequest(Long requestId, ApprovalActionRequest request);

    RequestDetailResponseDto returnRequest(Long requestId, ApprovalActionRequest request);

    // Pending Requests for Approvers
    List<RequestListResponseDto> getPendingAdminRequests();

    List<RequestListResponseDto> getPendingGmRequests();

    List<RequestListResponseDto> getPendingCeoRequests();

    // Comments and Attachments
    void addComment(Long requestId, String comment, boolean isInternal);

    void uploadAttachment(Long requestId, String fileName, String filePath, String contentType, long fileSize);

    List<CommentDto> getRequestComments(Long requestId);

    List<AttachmentDto> getRequestAttachments(Long requestId);

    AttachmentDownloadDto getRequestAttachmentDownload(Long requestId, Long attachmentId);

    // Utility Methods
    List<RequestListResponseDto> getOverdueRequests();

    long countRequestsByStatus(String status);

    boolean canUserActOnRequest(Long requestId, String action);
}
