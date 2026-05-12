package Ranaka.ranaka.notification.controller;

import Ranaka.ranaka.notification.dto.NotificationResponseDto;
import Ranaka.ranaka.notification.dto.PushTokenRevokeRequest;
import Ranaka.ranaka.notification.dto.PushTokenRegistrationRequest;
import Ranaka.ranaka.notification.dto.PushTokenRegistrationResponse;
import Ranaka.ranaka.notification.service.NotificationService;
import Ranaka.ranaka.notification.service.PushNotificationDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final PushNotificationDeviceService pushNotificationDeviceService;

    @GetMapping("/my")
    public ResponseEntity<Page<NotificationResponseDto>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // This powers screens like:
        // "Show me the latest notifications for the currently signed-in user."
        Page<NotificationResponseDto> response = notificationService.getMyNotifications(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponseDto> markAsRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead() {
        // Handy when a user opens the notification center and wants to clear the unread badge quickly.
        int updated = notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("updatedCount", updated));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount()));
    }

    @PostMapping("/push-token")
    public ResponseEntity<PushTokenRegistrationResponse> registerPushToken(
            @Valid @RequestBody PushTokenRegistrationRequest request) {
        return ResponseEntity.ok(pushNotificationDeviceService.registerPushToken(request));
    }

    @PostMapping("/push-token/unregister")
    public ResponseEntity<Map<String, Boolean>> unregisterPushToken(
            @Valid @RequestBody PushTokenRevokeRequest request) {
        pushNotificationDeviceService.unregisterPushToken(request.getToken());
        return ResponseEntity.ok(Map.of("success", true));
    }
}
