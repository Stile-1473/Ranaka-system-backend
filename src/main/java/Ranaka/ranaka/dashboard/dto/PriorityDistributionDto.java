package Ranaka.ranaka.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriorityDistributionDto {
    private Long criticalCount;
    private Long highCount;
    private Long mediumCount;
    private Long lowCount;
}
