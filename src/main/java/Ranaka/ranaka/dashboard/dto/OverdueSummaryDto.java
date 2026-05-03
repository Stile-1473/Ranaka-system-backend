package Ranaka.ranaka.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverdueSummaryDto {
    private Long totalOverdueRequests;
    private Long adminOverdueCount;
    private Long gmOverdueCount;
    private Long ceoOverdueCount;
    private Long criticalOverdueCount;
}
