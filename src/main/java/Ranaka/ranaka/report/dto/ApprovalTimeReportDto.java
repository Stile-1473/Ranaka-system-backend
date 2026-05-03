package Ranaka.ranaka.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTimeReportDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long completedRequestCount;
    private Double overallAverageHours;
    private Double adminAverageHours;
    private Double gmAverageHours;
    private Double ceoAverageHours;
}
