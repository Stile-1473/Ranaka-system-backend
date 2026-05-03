package Ranaka.ranaka.settings.serviceImpl;

import Ranaka.ranaka.audit.entity.AuditLog;
import Ranaka.ranaka.audit.repository.AuditLogRepository;
import Ranaka.ranaka.common.enums.AuditAction;
import Ranaka.ranaka.common.exception.ResourceNotFoundException;
import Ranaka.ranaka.settings.dto.PriorityConfigurationDto;
import Ranaka.ranaka.settings.dto.SLAConfigurationDto;
import Ranaka.ranaka.settings.dto.SystemSettingResponse;
import Ranaka.ranaka.settings.dto.UpdateSettingRequest;
import Ranaka.ranaka.settings.entity.SystemSetting;
import Ranaka.ranaka.settings.repository.SystemSettingRepository;
import Ranaka.ranaka.settings.service.SettingsService;
import Ranaka.ranaka.user.entity.User;
import Ranaka.ranaka.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SettingsServiceImpl implements SettingsService {

    private final SystemSettingRepository settingRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    // SLA = Service Level Agreement.
    // In this project it means the expected maximum time each approval stage should take.
    // Example: Admin should act within 24h, GM within 48h, CEO within 72h.
    // Default SLA hours
    private static final Integer DEFAULT_ADMIN_SLA = 24;
    private static final Integer DEFAULT_GM_SLA = 48;
    private static final Integer DEFAULT_CEO_SLA = 72;
    private static final Integer DEFAULT_REMINDER_HOURS = 2;

    // Default Priority SLA hours
    private static final Integer DEFAULT_CRITICAL_SLA = 4;
    private static final Integer DEFAULT_HIGH_SLA = 8;
    private static final Integer DEFAULT_MEDIUM_SLA = 24;
    private static final Integer DEFAULT_LOW_SLA = 48;

    @Override
    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getAllSettings() {
        log.info("Fetching all system settings");

        return settingRepository.findByDeletedAtIsNullOrderBySettingKeyAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SystemSettingResponse getSettingByKey(String settingKey) {
        log.info("Fetching setting with key: {}", settingKey);

        SystemSetting setting = settingRepository.findBySettingKeyAndDeletedAtIsNull(settingKey)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with key: " + settingKey));

        return mapToResponse(setting);
    }

    @Override
    public SystemSettingResponse updateSetting(String settingKey, UpdateSettingRequest request) {
        log.info("Updating setting with key: {}", settingKey);

        SystemSetting setting = settingRepository.findBySettingKeyAndDeletedAtIsNull(settingKey)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with key: " + settingKey));
        String oldValue = setting.getSettingValue();

        if (setting.isSystemSetting()) {
            log.warn("Attempt to update system setting: {}", settingKey);
            throw new IllegalArgumentException("Cannot modify system settings directly. Use specific update endpoints.");
        }

        if (request.getSettingValue() != null && !request.getSettingValue().isEmpty()) {
            setting.setSettingValue(request.getSettingValue());
        }

        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            setting.setDescription(request.getDescription());
        }

        SystemSetting updatedSetting = settingRepository.save(setting);
        logAuditAction(settingKey, oldValue, updatedSetting.getSettingValue(), "Setting updated");
        log.info("Successfully updated setting: {}", settingKey);

        return mapToResponse(updatedSetting);
    }

    @Override
    public SystemSettingResponse createOrUpdateSetting(String settingKey, String settingValue,
                                                      String description, String settingType) {
        log.info("Creating or updating setting with key: {}", settingKey);

        SystemSetting setting = settingRepository.findFirstBySettingKey(settingKey)
                .orElse(SystemSetting.builder()
                        .settingKey(settingKey)
                        .build());

        if (setting.getDeletedAt() != null) {
            setting.setDeletedAt(null);
        }

        setting.setSettingValue(settingValue);
        setting.setDescription(description);
        setting.setSettingType(settingType);

        SystemSetting savedSetting = settingRepository.save(setting);
        logAuditAction(settingKey, null, settingValue, "Setting created or updated");
        log.info("Successfully saved setting: {}", settingKey);

        return mapToResponse(savedSetting);
    }

    @Override
    public void deleteSetting(String settingKey) {
        log.info("Deleting setting with key: {}", settingKey);

        SystemSetting setting = settingRepository.findBySettingKeyAndDeletedAtIsNull(settingKey)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found with key: " + settingKey));

        if (setting.isSystemSetting()) {
            throw new IllegalArgumentException("Cannot delete system settings");
        }

        logAuditAction(settingKey, setting.getSettingValue(), null, "Setting archived");
        setting.setDeletedAt(LocalDateTime.now());
        settingRepository.save(setting);
        log.info("Successfully deleted setting: {}", settingKey);
    }

    @Override
    public SLAConfigurationDto getSLAConfiguration() {
        log.info("Fetching SLA configuration");

        // If values are missing from the database, we fall back to safe defaults
        // so the scheduler and dashboards still behave predictably.
        Integer adminSla = getSettingValueAsInteger("SLA_ADMIN_HOURS");
        Integer gmSla = getSettingValueAsInteger("SLA_GM_HOURS");
        Integer ceoSla = getSettingValueAsInteger("SLA_CEO_HOURS");
        Integer reminderHours = getSettingValueAsInteger("SLA_REMINDER_HOURS");

        return SLAConfigurationDto.builder()
                .adminSlaHours(adminSla != null ? adminSla : DEFAULT_ADMIN_SLA)
                .gmSlaHours(gmSla != null ? gmSla : DEFAULT_GM_SLA)
                .ceoSlaHours(ceoSla != null ? ceoSla : DEFAULT_CEO_SLA)
                .reminderHoursBeforeBreach(reminderHours != null ? reminderHours : DEFAULT_REMINDER_HOURS)
                .build();
    }

    @Override
    public SLAConfigurationDto updateSLAConfiguration(SLAConfigurationDto config) {
        log.info("Updating SLA configuration");


        // leadership says "Admin gets 24h, GM gets 48h, CEO gets 72h, remind 2h before breach."
        createOrUpdateSetting("SLA_ADMIN_HOURS", config.getAdminSlaHours().toString(),
                "SLA time limit for admin recommendation in hours", "INTEGER");
        createOrUpdateSetting("SLA_GM_HOURS", config.getGmSlaHours().toString(),
                "SLA time limit for GM approval in hours", "INTEGER");
        createOrUpdateSetting("SLA_CEO_HOURS", config.getCeoSlaHours().toString(),
                "SLA time limit for CEO authorization in hours", "INTEGER");
        createOrUpdateSetting("SLA_REMINDER_HOURS", config.getReminderHoursBeforeBreach().toString(),
                "Hours before SLA breach to send reminder", "INTEGER");

        log.info("Successfully updated SLA configuration");
        return getSLAConfiguration();
    }

    @Override
    public PriorityConfigurationDto getPriorityConfiguration() {
        log.info("Fetching priority configuration");

        Integer criticalSla = getSettingValueAsInteger("PRIORITY_CRITICAL_SLA_HOURS");
        Integer highSla = getSettingValueAsInteger("PRIORITY_HIGH_SLA_HOURS");
        Integer mediumSla = getSettingValueAsInteger("PRIORITY_MEDIUM_SLA_HOURS");
        Integer lowSla = getSettingValueAsInteger("PRIORITY_LOW_SLA_HOURS");

        return PriorityConfigurationDto.builder()
                .criticalSlaHours(criticalSla != null ? criticalSla : DEFAULT_CRITICAL_SLA)
                .highSlaHours(highSla != null ? highSla : DEFAULT_HIGH_SLA)
                .mediumSlaHours(mediumSla != null ? mediumSla : DEFAULT_MEDIUM_SLA)
                .lowSlaHours(lowSla != null ? lowSla : DEFAULT_LOW_SLA)
                .build();
    }

    @Override
    public PriorityConfigurationDto updatePriorityConfiguration(PriorityConfigurationDto config) {
        log.info("Updating priority configuration");

        // This lets the business tune how aggressively different priorities should be treated later.
        createOrUpdateSetting("PRIORITY_CRITICAL_SLA_HOURS", config.getCriticalSlaHours().toString(),
                "SLA hours for critical priority requests", "INTEGER");
        createOrUpdateSetting("PRIORITY_HIGH_SLA_HOURS", config.getHighSlaHours().toString(),
                "SLA hours for high priority requests", "INTEGER");
        createOrUpdateSetting("PRIORITY_MEDIUM_SLA_HOURS", config.getMediumSlaHours().toString(),
                "SLA hours for medium priority requests", "INTEGER");
        createOrUpdateSetting("PRIORITY_LOW_SLA_HOURS", config.getLowSlaHours().toString(),
                "SLA hours for low priority requests", "INTEGER");

        log.info("Successfully updated priority configuration");
        return getPriorityConfiguration();
    }

    @Override
    @Transactional(readOnly = true)
    public String getSettingValueAsString(String settingKey) {
        return settingRepository.findBySettingKeyAndDeletedAtIsNull(settingKey)
                .map(SystemSetting::getSettingValue)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getSettingValueAsInteger(String settingKey) {
        try {
            String value = getSettingValueAsString(settingKey);
            return value != null ? Integer.parseInt(value) : null;
        } catch (NumberFormatException e) {
            log.error("Invalid integer value for setting: {}", settingKey);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean getSettingValueAsBoolean(String settingKey) {
        try {
            String value = getSettingValueAsString(settingKey);
            return value != null ? Boolean.parseBoolean(value) : null;
        } catch (Exception e) {
            log.error("Invalid boolean value for setting: {}", settingKey);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean settingExists(String settingKey) {
        return settingRepository.existsBySettingKeyAndDeletedAtIsNull(settingKey);
    }

    @Override
    public void resetDefaultSettings() {
        log.info("Resetting settings to defaults");

        // Useful for demo recovery or when experimental config changes need to be rolled back quickly.
        // Reset SLA settings
        createOrUpdateSetting("SLA_ADMIN_HOURS", String.valueOf(DEFAULT_ADMIN_SLA),
                "SLA time limit for admin recommendation in hours", "INTEGER");
        createOrUpdateSetting("SLA_GM_HOURS", String.valueOf(DEFAULT_GM_SLA),
                "SLA time limit for GM approval in hours", "INTEGER");
        createOrUpdateSetting("SLA_CEO_HOURS", String.valueOf(DEFAULT_CEO_SLA),
                "SLA time limit for CEO authorization in hours", "INTEGER");
        createOrUpdateSetting("SLA_REMINDER_HOURS", String.valueOf(DEFAULT_REMINDER_HOURS),
                "Hours before SLA breach to send reminder", "INTEGER");

        // Reset priority settings
        createOrUpdateSetting("PRIORITY_CRITICAL_SLA_HOURS", String.valueOf(DEFAULT_CRITICAL_SLA),
                "SLA hours for critical priority requests", "INTEGER");
        createOrUpdateSetting("PRIORITY_HIGH_SLA_HOURS", String.valueOf(DEFAULT_HIGH_SLA),
                "SLA hours for high priority requests", "INTEGER");
        createOrUpdateSetting("PRIORITY_MEDIUM_SLA_HOURS", String.valueOf(DEFAULT_MEDIUM_SLA),
                "SLA hours for medium priority requests", "INTEGER");
        createOrUpdateSetting("PRIORITY_LOW_SLA_HOURS", String.valueOf(DEFAULT_LOW_SLA),
                "SLA hours for low priority requests", "INTEGER");

        logAuditAction("SYSTEM_SETTINGS", null, "DEFAULTS_RESTORED", "System settings reset to defaults");
        log.info("Successfully reset settings to defaults");
    }

    /**
     * Helper method to map SystemSetting entity to response DTO
     */
    private SystemSettingResponse mapToResponse(SystemSetting setting) {
        return SystemSettingResponse.builder()
                .id(setting.getId())
                .settingKey(setting.getSettingKey())
                .settingValue(setting.getSettingValue())
                .description(setting.getDescription())
                .settingType(setting.getSettingType())
                .isSystemSetting(setting.isSystemSetting())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }

    private void logAuditAction(String settingKey, String oldValue, String newValue, String description) {
        try {
            User currentUser = getCurrentUser();
            // Settings changes are operationally important, so they should leave an audit trail too.
            auditLogRepository.save(AuditLog.builder()
                    .user(currentUser)
                    .action(AuditAction.SETTINGS_CHANGED)
                    .description(description)
                    .entityType("SystemSetting")
                    .entityId(null)
                    .oldValue(oldValue)
                    .newValue(newValue != null ? settingKey + "=" + newValue : settingKey)
                    .ipAddress("system")
                    .userAgent("system")
                    .build());
        } catch (Exception e) {
            log.warn("Failed to audit settings change for {}: {}", settingKey, e.getMessage());
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }

        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
