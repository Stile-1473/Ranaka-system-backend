package Ranaka.ranaka.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnsRejectionsReportDto {
    private Long totalRequests;
    private Long returnedRequests;
    private Long rejectedRequests;
    private Double returnRatePercent;
    private Double rejectionRatePercent;
}
