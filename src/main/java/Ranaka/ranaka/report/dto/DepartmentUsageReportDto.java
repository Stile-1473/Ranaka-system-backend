package Ranaka.ranaka.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentUsageReportDto {
    private Long departmentId;
    private String departmentName;
    private Long totalRequests;
    private Long completedRequests;
    private Long rejectedRequests;
    private Long returnedRequests;
    private Long overdueRequests;
    private BigDecimal totalEstimatedCost;
    private BigDecimal averageEstimatedCost;
}
