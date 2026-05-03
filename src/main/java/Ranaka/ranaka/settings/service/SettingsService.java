package Ranaka.ranaka.settings.service;

import Ranaka.ranaka.settings.dto.PriorityConfigurationDto;
import Ranaka.ranaka.settings.dto.SLAConfigurationDto;
import Ranaka.ranaka.settings.dto.SystemSettingResponse;
import Ranaka.ranaka.settings.dto.UpdateSettingRequest;
import Ranaka.ranaka.settings.entity.SystemSetting;

import java.util.List;

public interface SettingsService {

    /**
     * Get all system settings
     */
    List<SystemSettingResponse> getAllSettings();

    /**
     * Get setting by key
     */
    SystemSettingResponse getSettingByKey(String settingKey);

    /**
     * Update a system setting
     */
    SystemSettingResponse updateSetting(String settingKey, UpdateSettingRequest request);

    /**
     * Create or update a setting
     */
    SystemSettingResponse createOrUpdateSetting(String settingKey, String settingValue, 
                                               String description, String settingType);

    /**
     * Delete a setting (cannot delete system settings)
     */
    void deleteSetting(String settingKey);

    /**
     * Get SLA configuration
     */
    SLAConfigurationDto getSLAConfiguration();

    /**
     * Update SLA configuration
     */
    SLAConfigurationDto updateSLAConfiguration(SLAConfigurationDto config);

    /**
     * Get priority configuration
     */
    PriorityConfigurationDto getPriorityConfiguration();

    /**
     * Update priority configuration
     */
    PriorityConfigurationDto updatePriorityConfiguration(PriorityConfigurationDto config);

    /**
     * Get setting value as String
     */
    String getSettingValueAsString(String settingKey);

    /**
     * Get setting value as Integer
     */
    Integer getSettingValueAsInteger(String settingKey);

    /**
     * Get setting value as Boolean
     */
    Boolean getSettingValueAsBoolean(String settingKey);

    /**
     * Check if setting exists
     */
    boolean settingExists(String settingKey);

    /**
     * Reset all settings to default
     */
    void resetDefaultSettings();
}
