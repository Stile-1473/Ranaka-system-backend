package Ranaka.ranaka.notification.service;

import Ranaka.ranaka.notification.dto.PushTokenRegistrationRequest;
import Ranaka.ranaka.notification.dto.PushTokenRegistrationResponse;

public interface PushNotificationDeviceService {

    PushTokenRegistrationResponse registerPushToken(PushTokenRegistrationRequest request);

    void unregisterPushToken(String token);
}
