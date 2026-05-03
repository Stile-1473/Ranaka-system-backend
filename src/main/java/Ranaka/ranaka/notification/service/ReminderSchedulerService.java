package Ranaka.ranaka.notification.service;

import Ranaka.ranaka.common.enums.ApprovalAction;
import Ranaka.ranaka.common.enums.NotificationType;
import Ranaka.ranaka.common.enums.RequestStatus;
import Ranaka.ranaka.common.enums.WorkflowStage;
import Ranaka.ranaka.request.entity.ProcurementRequest;
import Ranaka.ranaka.request.entity.RequestApproval;
import Ranaka.ranaka.request.repository.ProcurementRequestRepository;
import Ranaka.ranaka.request.repository.RequestApprovalRepository;
import Ranaka.ranaka.settings.dto.SLAConfigurationDto;
import Ranaka.ranaka.settings.service.SettingsService;
import Ranaka.ranaka.user.domain.Role;
import Ranaka.ranaka.user.entity.User;
import Ranaka.ranaka.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderSchedulerService {

    private final ProcurementRequestRepository requestRepository;
    private final RequestApprovalRepository approvalRepository;
    private final SettingsService settingsService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Scheduled(fixedDelayString = "${workflow.deadline-check-ms:300000}")
    @Transactional
    public void processWorkflowDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        SLAConfigurationDto sla = settingsService.getSLAConfiguration();

        // SLA windows drive the reminder and overdue process.
        // Example: if GM SLA is 48h, we warn shortly before 48h and mark overdue after that point.
        // We only inspect requests that are actively waiting on an approver.
        List<ProcurementRequest> activeRequests = requestRepository.findAll().stream()
                .filter(this::isActiveWorkflowRequest)
                .toList();

        for (ProcurementRequest request : activeRequests) {
            WorkflowStage stage = request.getCurrentStage();
            LocalDateTime stageStartedAt = resolveStageStartedAt(request, stage).orElse(request.getSubmittedAt());
            if (stageStartedAt == null) {
                continue;
            }

            int stageSlaHours = getStageSlaHours(stage, sla);
            LocalDateTime dueAt = stageStartedAt.plusHours(stageSlaHours);
            LocalDateTime reminderAt = dueAt.minusHours(Math.min(stageSlaHours, sla.getReminderHoursBeforeBreach()));

            // Example:
            // if Admin SLA is 24 hours and reminder is 2 hours before breach,
            // the reminder should happen around hour 22.
            if (!Boolean.TRUE.equals(request.getIsOverdue()) && !now.isBefore(reminderAt) && now.isBefore(dueAt)) {
                sendReminderNotifications(request, dueAt);
            }

            if (!now.isBefore(dueAt)) {
                markOverdueAndNotify(request, dueAt);
            }
        }
    }

    private boolean isActiveWorkflowRequest(ProcurementRequest request) {
        return request.getStatus() == RequestStatus.PENDING_ADMIN_RECOMMENDATION
                || request.getStatus() == RequestStatus.PENDING_GM_APPROVAL
                || request.getStatus() == RequestStatus.PENDING_CEO_AUTHORIZATION;
    }

    private int getStageSlaHours(WorkflowStage stage, SLAConfigurationDto sla) {
        return switch (stage) {
            case ADMIN_RECOMMENDATION -> sla.getAdminSlaHours();
            case GM_APPROVAL -> sla.getGmSlaHours();
            case CEO_AUTHORIZATION -> sla.getCeoSlaHours();
            default -> sla.getAdminSlaHours();
        };
    }

    private Optional<LocalDateTime> resolveStageStartedAt(ProcurementRequest request, WorkflowStage stage) {
        if (stage == WorkflowStage.ADMIN_RECOMMENDATION) {
            return Optional.ofNullable(request.getSubmittedAt());
        }

        List<RequestApproval> approvals = approvalRepository.findByRequestIdOrderByCreatedAtDesc(request.getId());
        return approvals.stream()
                .filter(approval -> matchesStageEntry(stage, approval))
                .map(approval -> approval.getActionDate() != null ? approval.getActionDate() : approval.getCreatedAt())
                .findFirst();
    }

    private boolean matchesStageEntry(WorkflowStage currentStage, RequestApproval approval) {
        return (currentStage == WorkflowStage.GM_APPROVAL
                && approval.getStage() == WorkflowStage.ADMIN_RECOMMENDATION
                && approval.getAction() == ApprovalAction.RECOMMEND)
                || (currentStage == WorkflowStage.CEO_AUTHORIZATION
                && approval.getStage() == WorkflowStage.GM_APPROVAL
                && approval.getAction() == ApprovalAction.APPROVE);
    }

    private void sendReminderNotifications(ProcurementRequest request, LocalDateTime dueAt) {
        for (User recipient : getCurrentStageRecipients(request.getCurrentStage())) {
            if (notificationService.hasNotification(
                    recipient.getId(),
                    NotificationType.REMINDER_TRIGGERED,
                    request.getId(),
                    "ProcurementRequest")) {
                continue;
            }

            notificationService.createNotification(
                    recipient,
                    NotificationType.REMINDER_TRIGGERED,
                    "Reminder: Request action due soon",
                    String.format("Procurement request '%s' is approaching its SLA deadline at %s.",
                            request.getTitle(),
                            dueAt),
                    request.getId(),
                    "ProcurementRequest"
            );
        }
    }

    private void markOverdueAndNotify(ProcurementRequest request, LocalDateTime dueAt) {
        if (!Boolean.TRUE.equals(request.getIsOverdue())) {
            // Once overdue, the request becomes visible in overdue queues and dashboards.
            request.setIsOverdue(true);
            request.setOverdueAt(dueAt);
            requestRepository.save(request);
        }

        for (User recipient : getEscalationRecipients(request)) {
            if (notificationService.hasNotification(
                    recipient.getId(),
                    NotificationType.REQUEST_OVERDUE,
                    request.getId(),
                    "ProcurementRequest")) {
                continue;
            }

            notificationService.createNotification(
                    recipient,
                    NotificationType.REQUEST_OVERDUE,
                    "Overdue procurement request",
                    String.format("Procurement request '%s' is overdue at stage %s and needs immediate action.",
                            request.getTitle(),
                            request.getCurrentStage()),
                    request.getId(),
                    "ProcurementRequest"
            );
        }
    }

    private List<User> getCurrentStageRecipients(WorkflowStage stage) {
        return switch (stage) {
            case ADMIN_RECOMMENDATION -> userRepository.findByRole(Role.ADMIN);
            case GM_APPROVAL -> userRepository.findByRole(Role.GM);
            case CEO_AUTHORIZATION -> userRepository.findByRole(Role.CEO);
            default -> List.of();
        };
    }

    private List<User> getEscalationRecipients(ProcurementRequest request) {
        // Overdue escalations go wider than normal reminders:
        // current stage owners + system admins + the original requester.
        List<User> stageOwners = getCurrentStageRecipients(request.getCurrentStage());
        List<User> systemAdmins = userRepository.findByRole(Role.SYSTEM_ADMIN);
        List<User> recipients = new java.util.ArrayList<>(stageOwners);
        recipients.addAll(systemAdmins);
        recipients.add(request.getRequester());
        return recipients.stream().distinct().toList();
    }
}
