package Ranaka.ranaka.notification.service;

import Ranaka.ranaka.common.enums.NotificationType;
import Ranaka.ranaka.notification.dto.NotificationResponseDto;
import Ranaka.ranaka.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    NotificationResponseDto createNotification(User recipient,
                                               NotificationType type,
                                               String title,
                                               String message,
                                               Long referenceId,
                                               String referenceType);

    Page<NotificationResponseDto> getMyNotifications(Pageable pageable);

    NotificationResponseDto markAsRead(Long notificationId);

    int markAllAsRead();

    long getUnreadCount();

    boolean hasNotification(Long recipientId, NotificationType type, Long referenceId, String referenceType);
}
