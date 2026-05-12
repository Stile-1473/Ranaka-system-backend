package Ranaka.ranaka.notification.serviceImpl;

import Ranaka.ranaka.common.exception.ResourceNotFoundException;
import Ranaka.ranaka.notification.dto.PushTokenRegistrationRequest;
import Ranaka.ranaka.notification.dto.PushTokenRegistrationResponse;
import Ranaka.ranaka.notification.entity.PushToken;
import Ranaka.ranaka.notification.repository.PushTokenRepository;
import Ranaka.ranaka.notification.service.PushNotificationDeviceService;
import Ranaka.ranaka.user.entity.User;
import Ranaka.ranaka.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class PushNotificationDeviceServiceImpl implements PushNotificationDeviceService {

    private final PushTokenRepository pushTokenRepository;
    private final UserRepository userRepository;

    @Override
    public PushTokenRegistrationResponse registerPushToken(PushTokenRegistrationRequest request) {
        String token = request.getToken().trim();
        if (!isExpoPushToken(token)) {
            throw new IllegalArgumentException("Unsupported push token format");
        }

        User currentUser = getCurrentUser();
        PushToken pushToken = pushTokenRepository.findByToken(token)
                .orElseGet(PushToken::new);

        pushToken.setUser(currentUser);
        pushToken.setToken(token);
        pushToken.setPlatform(request.getPlatform().trim().toUpperCase(Locale.ROOT));
        pushToken.setDeviceName(cleanDeviceName(request.getDeviceName(), request.getPlatform()));
        pushToken.setActive(true);
        pushToken.setLastRegisteredAt(LocalDateTime.now());

        PushToken savedToken = pushTokenRepository.save(pushToken);

        return PushTokenRegistrationResponse.builder()
                .registered(true)
                .platform(savedToken.getPlatform())
                .deviceName(savedToken.getDeviceName())
                .registeredAt(savedToken.getLastRegisteredAt())
                .build();
    }

    @Override
    public void unregisterPushToken(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        User currentUser = getCurrentUser();
        pushTokenRepository.findByToken(token.trim()).ifPresent(pushToken -> {
            if (pushToken.getUser() != null && pushToken.getUser().getId().equals(currentUser.getId())) {
                pushToken.setActive(false);
                pushTokenRepository.save(pushToken);
            }
        });
    }

    private boolean isExpoPushToken(String token) {
        return token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken[");
    }

    private String cleanDeviceName(String deviceName, String platform) {
        if (StringUtils.hasText(deviceName)) {
            return deviceName.trim();
        }
        return platform == null ? "mobile" : platform.trim().toLowerCase(Locale.ROOT);
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
