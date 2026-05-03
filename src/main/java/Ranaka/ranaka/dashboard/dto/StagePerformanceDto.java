package Ranaka.ranaka.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StagePerformanceDto {
    private String stageName;
    private Long pendingCount;
    private Long completedCount;
    private Double avgProcessingTimeHours;
}
