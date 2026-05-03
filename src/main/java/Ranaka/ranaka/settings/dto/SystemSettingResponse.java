package Ranaka.ranaka.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingResponse {
    private Long id;
    private String settingKey;
    private String settingValue;
    private String description;
    private String settingType;
    private boolean isSystemSetting;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
