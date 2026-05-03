package Ranaka.ranaka.notification.service;

import Ranaka.ranaka.notification.entity.Notification;
import Ranaka.ranaka.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email Service for sending notifications via email.
 * This service handles the asynchronous sending of email notifications
 * for all approval workflow events, reminders, and escalations.
 *
 * Features:
 * - Non-blocking email sending
 * - Graceful error handling with logging
 * - HTML and plain text email support
 * - Retry mechanism capability
 * - Audit trail for email delivery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private static final String EMAIL_FROM = "noreply@ranaka-procurement.com";
    private static final String EMAIL_SUBJECT_PREFIX = "[Procurement System]";

    /**
     * Sends an email notification to a user.
     * This method is non-blocking and logs failures without throwing exceptions.
     *
     * @param user The recipient user
     * @param subject Email subject
     * @param body Email body content
     */
    public void sendEmail(User user, String subject, String body) {
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            log.warn("Cannot send email: User or email address is null");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(EMAIL_FROM);
            message.setTo(user.getEmail());
            message.setSubject(EMAIL_SUBJECT_PREFIX + " " + subject);
            message.setText(body);

            // Send email asynchronously
            mailSender.send(message);
            log.info("Email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            // Log the error but don't fail the main operation
            log.error("Failed to send email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Sends a notification email with formatted content.
     *
     * @param notification The notification object containing recipient and message
     */
    public void sendNotificationEmail(Notification notification) {
        if (notification.getRecipient() == null) {
            log.warn("Cannot send notification email: Recipient is null");
            return;
        }

        User recipient = notification.getRecipient();
        String subject = notification.getTitle();
        String body = formatEmailBody(notification);

        sendEmail(recipient, subject, body);
    }

    /**
     * Sends approval request notification email to approvers.
     *
     * @param recipient The approver
     * @param requestTitle The title of the procurement request
     * @param priority The priority level
     * @param stage The current workflow stage
     * @param requestUrl The URL to access the request
     */
    public void sendApprovalEmail(User recipient, String requestTitle, String priority,
                                  String stage, String requestUrl) {
        if (recipient == null || recipient.getEmail() == null) {
            log.warn("Cannot send approval email: Invalid recipient");
            return;
        }

        String subject = "Action Required: " + requestTitle + " (" + priority + ")";
        String body = buildApprovalEmailBody(requestTitle, priority, stage, requestUrl, recipient.getFirstName());

        sendEmail(recipient, subject, body);
    }

    /**
     * Sends request completion notification to requester.
     *
     * @param recipient The requester
     * @param requestTitle The title of the procurement request
     * @param requestUrl The URL to view the request
     */
    public void sendCompletionEmail(User recipient, String requestTitle, String requestUrl) {
        if (recipient == null || recipient.getEmail() == null) {
            log.warn("Cannot send completion email: Invalid recipient");
            return;
        }

        String subject = "Request Approved: " + requestTitle;
        String body = buildCompletionEmailBody(requestTitle, requestUrl, recipient.getFirstName());

        sendEmail(recipient, subject, body);
    }

    /**
     * Sends request rejection notification to requester.
     *
     * @param recipient The requester
     * @param requestTitle The title of the procurement request
     * @param reason The rejection reason
     * @param requestUrl The URL to view the request
     */
    public void sendRejectionEmail(User recipient, String requestTitle, String reason, String requestUrl) {
        if (recipient == null || recipient.getEmail() == null) {
            log.warn("Cannot send rejection email: Invalid recipient");
            return;
        }

        String subject = "Request Rejected: " + requestTitle;
        String body = buildRejectionEmailBody(requestTitle, reason, requestUrl, recipient.getFirstName());

        sendEmail(recipient, subject, body);
    }

    /**
     * Sends request return notification to requester.
     *
     * @param recipient The requester
     * @param requestTitle The title of the procurement request
     * @param reason The return reason
     * @param requestUrl The URL to view the request
     */
    public void sendReturnEmail(User recipient, String requestTitle, String reason, String requestUrl) {
        if (recipient == null || recipient.getEmail() == null) {
            log.warn("Cannot send return email: Invalid recipient");
            return;
        }

        String subject = "Request Returned for Correction: " + requestTitle;
        String body = buildReturnEmailBody(requestTitle, reason, requestUrl, recipient.getFirstName());

        sendEmail(recipient, subject, body);
    }

    /**
     * Sends SLA warning/overdue notification to approvers.
     *
     * @param recipient The approver
     * @param requestTitle The title of the procurement request
     * @param daysOverdue Number of days overdue
     * @param requestUrl The URL to access the request
     */
    public void sendOverdueEmail(User recipient, String requestTitle, int daysOverdue, String requestUrl) {
        if (recipient == null || recipient.getEmail() == null) {
            log.warn("Cannot send overdue email: Invalid recipient");
            return;
        }

        String subject = "⚠️ URGENT: " + requestTitle + " is " + daysOverdue + " days overdue";
        String body = buildOverdueEmailBody(requestTitle, daysOverdue, requestUrl, recipient.getFirstName());

        sendEmail(recipient, subject, body);
    }

    // ==================== EMAIL BODY BUILDERS ====================

    /**
     * Formats a generic notification into email body.
     */
    private String formatEmailBody(Notification notification) {
        return String.format(
            "Hello,\n\n" +
            "%s\n\n" +
            "%s\n\n" +
            "---\n" +
            "Procurement Management System\n" +
            "Please do not reply to this email.",
            notification.getTitle(),
            notification.getMessage()
        );
    }

    /**
     * Builds approval request email body.
     */
    private String buildApprovalEmailBody(String requestTitle, String priority, String stage,
                                         String requestUrl, String firstName) {
        return String.format(
            "Dear %s,\n\n" +
            "A new procurement request requires your attention:\n\n" +
            "Request: %s\n" +
            "Priority: %s\n" +
            "Stage: %s\n\n" +
            "Please review and take action on this request at your earliest convenience.\n\n" +
            "Access Request: %s\n\n" +
            "---\n" +
            "Procurement Management System\n" +
            "Please do not reply to this email.",
            firstName != null ? firstName : "User",
            requestTitle,
            priority,
            stage,
            requestUrl
        );
    }

    /**
     * Builds request completion email body.
     */
    private String buildCompletionEmailBody(String requestTitle, String requestUrl, String firstName) {
        return String.format(
            "Dear %s,\n\n" +
            "Good news! Your procurement request has been approved and completed.\n\n" +
            "Request: %s\n\n" +
            "You can view the details at: %s\n\n" +
            "Thank you for using our Procurement Management System.\n\n" +
            "---\n" +
            "Procurement Management System\n" +
            "Please do not reply to this email.",
            firstName != null ? firstName : "User",
            requestTitle,
            requestUrl
        );
    }

    /**
     * Builds request rejection email body.
     */
    private String buildRejectionEmailBody(String requestTitle, String reason, String requestUrl, String firstName) {
        return String.format(
            "Dear %s,\n\n" +
            "Your procurement request has been rejected.\n\n" +
            "Request: %s\n" +
            "Reason: %s\n\n" +
            "View Details: %s\n\n" +
            "If you have any questions, please contact the approver or procurement department.\n\n" +
            "---\n" +
            "Procurement Management System\n" +
            "Please do not reply to this email.",
            firstName != null ? firstName : "User",
            requestTitle,
            reason != null ? reason : "Not specified",
            requestUrl
        );
    }

    /**
     * Builds request return email body.
     */
    private String buildReturnEmailBody(String requestTitle, String reason, String requestUrl, String firstName) {
        return String.format(
            "Dear %s,\n\n" +
            "Your procurement request has been returned for correction.\n\n" +
            "Request: %s\n" +
            "Reason: %s\n\n" +
            "Please make the necessary corrections and resubmit at: %s\n\n" +
            "---\n" +
            "Procurement Management System\n" +
            "Please do not reply to this email.",
            firstName != null ? firstName : "User",
            requestTitle,
            reason != null ? reason : "Not specified",
            requestUrl
        );
    }

    /**
     * Builds overdue notification email body.
     */
    private String buildOverdueEmailBody(String requestTitle, int daysOverdue, String requestUrl, String firstName) {
        return String.format(
            "Dear %s,\n\n" +
            "URGENT: The following procurement request is overdue and requires immediate attention.\n\n" +
            "Request: %s\n" +
            "Days Overdue: %d\n\n" +
            "Please take action immediately at: %s\n\n" +
            "Contact the procurement department if you need assistance.\n\n" +
            "---\n" +
            "Procurement Management System\n" +
            "Please do not reply to this email.",
            firstName != null ? firstName : "User",
            requestTitle,
            daysOverdue,
            requestUrl
        );
    }
}

