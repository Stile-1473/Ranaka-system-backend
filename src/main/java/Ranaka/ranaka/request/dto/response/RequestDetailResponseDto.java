package Ranaka.ranaka.request.dto.response;

import Ranaka.ranaka.common.enums.RequestPriority;
import Ranaka.ranaka.common.enums.RequestStatus;
import Ranaka.ranaka.common.enums.WorkflowStage;
import Ranaka.ranaka.request.dto.LineItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDetailResponseDto {

    private Long id;
    private String title;
    private String description;
    private List<LineItemResponse> lineItems;
    private BigDecimal estimatedCost;
    private String departmentName;
    private Long departmentId;
    private String justification;
    private RequestPriority priority;
    private LocalDate requiredByDate;
    private RequestStatus status;
    private WorkflowStage currentStage;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private boolean isOverdue;
    private LocalDateTime overdueAt;
    private Integer returnCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Requester info
    private Long requesterId;
    private String requesterName;
    private String requesterEmail;

    // Approval history
    private List<ApprovalHistoryDto> approvalHistory;

    // Comments
    private List<CommentDto> comments;

    // Attachments
    private List<AttachmentDto> attachments;
}

