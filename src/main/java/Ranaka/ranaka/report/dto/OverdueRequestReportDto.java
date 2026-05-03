package Ranaka.ranaka.report.dto;

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
public class OverdueRequestReportDto {
    private Long requestId;
    private String title;
    private String requesterName;
    private String departmentName;
    private RequestPriority priority;
    private RequestStatus status;
    private WorkflowStage currentStage;
    private BigDecimal estimatedCost;
    private LocalDate requiredByDate;
    private LocalDateTime submittedAt;
    private LocalDateTime overdueAt;
    private Long ageHours;
}
