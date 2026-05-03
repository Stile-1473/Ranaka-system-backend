package Ranaka.ranaka.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BottleneckReportDto {
    private String stageName;
    private Long requestCount;
    private Long pendingCount;
    private Long overdueCount;
    private Double averageProcessingHours;
    private Double maxProcessingHours;
}
