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
public class RequestTrendDto {
    private LocalDate date;
    private Long submittedCount;
    private Long completedCount;
    private Long rejectedCount;
    private Long returnedCount;
}
