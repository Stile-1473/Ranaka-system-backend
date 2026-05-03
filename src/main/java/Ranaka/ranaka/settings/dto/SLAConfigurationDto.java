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
public class SLAConfigurationDto {
    
    @NotNull(message = "Admin SLA hours is required")
    @Min(value = 1, message = "Admin SLA must be at least 1 hour")
    private Integer adminSlaHours;
    
    @NotNull(message = "GM SLA hours is required")
    @Min(value = 1, message = "GM SLA must be at least 1 hour")
    private Integer gmSlaHours;
    
    @NotNull(message = "CEO SLA hours is required")
    @Min(value = 1, message = "CEO SLA must be at least 1 hour")
    private Integer ceoSlaHours;
    
    @NotNull(message = "Reminder hours before SLA breach is required")
    @Min(value = 1, message = "Reminder hours must be at least 1 hour")
    private Integer reminderHoursBeforeBreach;
}
