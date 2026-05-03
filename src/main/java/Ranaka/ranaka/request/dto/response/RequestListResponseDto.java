package Ranaka.ranaka.request.dto.response;

import Ranaka.ranaka.common.enums.RequestPriority;
import Ranaka.ranaka.common.enums.RequestStatus;
import Ranaka.ranaka.common.enums.WorkflowStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestListResponseDto {

    private Long id;
    private String title;
    private RequestStatus status;
    private WorkflowStage currentStage;
    private RequestPriority priority;
    private BigDecimal estimatedCost;
    private LocalDate requiredByDate;
    private String requesterName;
    private String departmentName;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private boolean isOverdue;
    private Integer returnCount;
}

