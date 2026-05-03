package Ranaka.ranaka.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {
    private Long totalRequests;
    private Long pendingRequests;
    private Long overdueRequests;
    private Long completedRequests;
    private Long rejectedRequests;
    private Long returnedRequests;
    private Double avgApprovalTimeHours;
    private LocalDate generatedDate;
}
