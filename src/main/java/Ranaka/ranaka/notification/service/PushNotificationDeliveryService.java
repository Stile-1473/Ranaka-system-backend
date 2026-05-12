package Ranaka.ranaka.notification.service;

import Ranaka.ranaka.notification.dto.NotificationResponseDto;
import Ranaka.ranaka.user.entity.User;

public interface PushNotificationDeliveryService {

    void deliverNotification(User recipient, NotificationResponseDto notification);
}
