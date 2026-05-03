package Ranaka.ranaka.settings.controller;

import Ranaka.ranaka.settings.dto.PriorityConfigurationDto;
import Ranaka.ranaka.settings.dto.SLAConfigurationDto;
import Ranaka.ranaka.settings.dto.SystemSettingResponse;
import Ranaka.ranaka.settings.dto.UpdateSettingRequest;
import Ranaka.ranaka.settings.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for System Settings Management
 * Provides endpoints for configuring SLA, priorities, and other system settings
 * Accessible only by SYSTEM_ADMIN role
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * Get all system settings
     * GET /api/v1/settings
     */
    @GetMapping
    public ResponseEntity<List<SystemSettingResponse>> getAllSettings() {
        log.info("Fetching all system settings");
        List<SystemSettingResponse> settings = settingsService.getAllSettings();
        return ResponseEntity.ok(settings);
    }

    /**
     * Get a specific setting by key
     * GET /api/v1/settings/{settingKey}
     */
    @GetMapping("/{settingKey}")
    public ResponseEntity<SystemSettingResponse> getSettingByKey(@PathVariable String settingKey) {
        log.info("Fetching setting with key: {}", settingKey);
        SystemSettingResponse setting = settingsService.getSettingByKey(settingKey);
        return ResponseEntity.ok(setting);
    }

    /**
     * Update a specific setting
     * PUT /api/v1/settings/{settingKey}
     */
    @PutMapping("/{settingKey}")
    public ResponseEntity<SystemSettingResponse> updateSetting(
            @PathVariable String settingKey,
            @Valid @RequestBody UpdateSettingRequest request) {
        log.info("Updating setting with key: {}", settingKey);
        SystemSettingResponse updated = settingsService.updateSetting(settingKey, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a setting (non-system settings only)
     * DELETE /api/v1/settings/{settingKey}
     */
    @DeleteMapping("/{settingKey}")
    public ResponseEntity<Map<String, String>> deleteSetting(@PathVariable String settingKey) {
        log.info("Deleting setting with key: {}", settingKey);
        try {
            settingsService.deleteSetting(settingKey);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Setting deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== SLA CONFIGURATION ====================

    /**
     * Get SLA configuration
     * GET /api/v1/settings/sla/config
     */
    @GetMapping("/sla/config")
    public ResponseEntity<SLAConfigurationDto> getSLAConfiguration() {
        log.info("Fetching SLA configuration");
        SLAConfigurationDto config = settingsService.getSLAConfiguration();
        return ResponseEntity.ok(config);
    }

    /**
     * Update SLA configuration
     * PUT /api/v1/settings/sla/config
     */
    @PutMapping("/sla/config")
    public ResponseEntity<SLAConfigurationDto> updateSLAConfiguration(
            @Valid @RequestBody SLAConfigurationDto config) {
        log.info("Updating SLA configuration");
        SLAConfigurationDto updated = settingsService.updateSLAConfiguration(config);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/sla")
    public ResponseEntity<SLAConfigurationDto> getSlaAlias() {
        // Alias kept so the API matches the project plan more closely.
        return getSLAConfiguration();
    }

    @PutMapping("/sla")
    public ResponseEntity<SLAConfigurationDto> updateSlaAlias(@Valid @RequestBody SLAConfigurationDto config) {
        return updateSLAConfiguration(config);
    }

    // ==================== PRIORITY CONFIGURATION ====================

    /**
     * Get priority configuration
     * GET /api/v1/settings/priority/config
     */
    @GetMapping("/priority/config")
    public ResponseEntity<PriorityConfigurationDto> getPriorityConfiguration() {
        log.info("Fetching priority configuration");
        PriorityConfigurationDto config = settingsService.getPriorityConfiguration();
        return ResponseEntity.ok(config);
    }

    /**
     * Update priority configuration
     * PUT /api/v1/settings/priority/config
     */
    @PutMapping("/priority/config")
    public ResponseEntity<PriorityConfigurationDto> updatePriorityConfiguration(
            @Valid @RequestBody PriorityConfigurationDto config) {
        log.info("Updating priority configuration");
        PriorityConfigurationDto updated = settingsService.updatePriorityConfiguration(config);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/priorities")
    public ResponseEntity<PriorityConfigurationDto> getPrioritiesAlias() {
        // Alias kept so frontend code can use the friendlier planned path.
        return getPriorityConfiguration();
    }

    @PutMapping("/priorities")
    public ResponseEntity<PriorityConfigurationDto> updatePrioritiesAlias(
            @Valid @RequestBody PriorityConfigurationDto config) {
        return updatePriorityConfiguration(config);
    }

    // ==================== SYSTEM OPERATIONS ====================

    /**
     * Reset all settings to default values
     * POST /api/v1/settings/reset-defaults
     */
    @PostMapping("/reset-defaults")
    public ResponseEntity<Map<String, String>> resetDefaultSettings() {
        log.info("Resetting all settings to defaults");
        try {
            settingsService.resetDefaultSettings();
            Map<String, String> response = new HashMap<>();
            response.put("message", "All settings have been reset to defaults");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resetting settings: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to reset settings"));
        }
    }

    /**
     * Get overall system configuration
     * GET /api/v1/settings/system-config
     */
    @GetMapping("/system-config")
    public ResponseEntity<Map<String, Object>> getSystemConfiguration() {
        log.info("Fetching overall system configuration");

        Map<String, Object> config = new HashMap<>();
        config.put("sla", settingsService.getSLAConfiguration());
        config.put("priority", settingsService.getPriorityConfiguration());
        config.put("lastUpdated", java.time.LocalDateTime.now());

        return ResponseEntity.ok(config);
    }

    @GetMapping("/workflow")
    public ResponseEntity<Map<String, Object>> getWorkflowConfiguration() {
        // Version 1 workflow is intentionally fixed:
        // Requester -> Admin -> GM -> CEO -> Completed.
        Map<String, Object> workflow = new HashMap<>();
        workflow.put("stages", List.of("DRAFT", "ADMIN_RECOMMENDATION", "GM_APPROVAL", "CEO_AUTHORIZATION", "COMPLETED"));
        workflow.put("stageSkippingAllowed", false);
        workflow.put("finalStatus", "COMPLETED");
        return ResponseEntity.ok(workflow);
    }

    @PutMapping("/workflow")
    public ResponseEntity<Map<String, String>> updateWorkflowConfiguration() {
        return ResponseEntity.ok(Map.of(
                "message", "Workflow is fixed in version 1 and cannot be changed dynamically"
        ));
    }
}
