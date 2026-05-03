package Ranaka.ranaka.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentStatsDto {
    private String departmentName;
    private Long totalRequests;
    private Long pendingRequests;
    private Long completedRequests;
    private Long rejectedRequests;
    private Double avgApprovalTime;
}
