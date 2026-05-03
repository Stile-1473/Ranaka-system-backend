package Ranaka.ranaka.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSettingRequest {
    
    @NotBlank(message = "Setting value is required")
    @Size(max = 1000, message = "Setting value must not exceed 1000 characters")
    private String settingValue;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
