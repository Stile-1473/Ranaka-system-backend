package Ranaka.ranaka.settings.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriorityConfigurationDto {
    
    @NotNull(message = "Critical priority SLA hours is required")
    @Min(value = 1, message = "SLA must be at least 1 hour")
    private Integer criticalSlaHours;
    
    @NotNull(message = "High priority SLA hours is required")
    @Min(value = 1, message = "SLA must be at least 1 hour")
    private Integer highSlaHours;
    
    @NotNull(message = "Medium priority SLA hours is required")
    @Min(value = 1, message = "SLA must be at least 1 hour")
    private Integer mediumSlaHours;
    
    @NotNull(message = "Low priority SLA hours is required")
    @Min(value = 1, message = "SLA must be at least 1 hour")
    private Integer lowSlaHours;
}
