package Ranaka.ranaka.notification.serviceImpl;

import Ranaka.ranaka.common.enums.NotificationType;
import Ranaka.ranaka.common.exception.ResourceNotFoundException;
import Ranaka.ranaka.notification.dto.NotificationResponseDto;
import Ranaka.ranaka.notification.entity.Notification;
import Ranaka.ranaka.notification.repository.NotificationRepository;
import Ranaka.ranaka.notification.service.EmailService;
import Ranaka.ranaka.notification.service.NotificationService;
import Ranaka.ranaka.notification.service.PushNotificationDeliveryService;
import Ranaka.ranaka.user.entity.User;
import Ranaka.ranaka.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationDeliveryService pushNotificationDeliveryService;

    @Override
    public NotificationResponseDto createNotification(User recipient,
                                                      NotificationType type,
                                                      String title,
                                                      String message,
                                                      Long referenceId,
                                                      String referenceType) {
        if (recipient == null) {
            throw new IllegalArgumentException("Notification recipient is required");
        }

        // Step 1: persist the in-app notification so the user can still see it later
        // even if email or WebSocket delivery fails.
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .isRead(false)
                .emailSent(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        try {
            // Step 2: try email delivery as a secondary channel.
            emailService.sendNotificationEmail(savedNotification);
            savedNotification.setEmailSent(true);
            savedNotification.setEmailSentAt(LocalDateTime.now());
            savedNotification = notificationRepository.save(savedNotification);
        } catch (Exception ex) {
            log.warn("Failed to send notification email to {}: {}", recipient.getEmail(), ex.getMessage());
        }

        NotificationResponseDto response = mapToDto(savedNotification);
        // Step 3: push the event to live subscribers so the UI updates immediately.
        publishRealtimeUpdate(recipient, response);
        // Step 4: send a real device push so the user can still see the update while the app is backgrounded.
        pushNotificationDeliveryService.deliverNotification(recipient, response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getMyNotifications(Pageable pageable) {
        User currentUser = getCurrentUser();
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(this::mapToDto);
    }

    @Override
    public NotificationResponseDto markAsRead(Long notificationId) {
        User currentUser = getCurrentUser();
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
            publishUnreadCount(currentUser);
        }

        return mapToDto(notification);
    }

    @Override
    public int markAllAsRead() {
        User currentUser = getCurrentUser();
        int updated = notificationRepository.markAllAsReadByRecipientId(currentUser.getId(), LocalDateTime.now());
        publishUnreadCount(currentUser);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User currentUser = getCurrentUser();
        return notificationRepository.countByRecipientIdAndIsReadFalse(currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasNotification(Long recipientId, NotificationType type, Long referenceId, String referenceType) {
        return notificationRepository.existsByRecipientIdAndTypeAndReferenceIdAndReferenceType(
                recipientId,
                type,
                referenceId,
                referenceType
        );
    }

    private NotificationResponseDto mapToDto(Notification notification) {
        return NotificationResponseDto.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .read(notification.isRead())
                .readAt(notification.getReadAt())
                .emailSent(notification.isEmailSent())
                .emailSentAt(notification.getEmailSentAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private void publishRealtimeUpdate(User recipient, NotificationResponseDto notification) {
        try {
            // Personal queue for the signed-in user.
            messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/notifications", notification);
            // Topic fallback is handy for debugging or simpler clients that subscribe by user id.
            messagingTemplate.convertAndSend("/topic/notifications/" + recipient.getId(), notification);
            publishUnreadCount(recipient);
        } catch (Exception ex) {
            log.warn("Failed to publish realtime notification for {}: {}", recipient.getEmail(), ex.getMessage());
        }
    }

    private void publishUnreadCount(User user) {
        long unreadCount = notificationRepository.countByRecipientIdAndIsReadFalse(user.getId());
        // Frontend example:
        // "Tariro has 5 unread notifications" can update instantly without a page refresh.
        messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications/unread-count", unreadCount);
        messagingTemplate.convertAndSend("/topic/notifications/" + user.getId() + "/unread-count", unreadCount);
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
