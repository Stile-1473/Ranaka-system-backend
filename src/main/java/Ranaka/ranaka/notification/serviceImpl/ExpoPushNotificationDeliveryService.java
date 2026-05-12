package Ranaka.ranaka.notification.serviceImpl;

import Ranaka.ranaka.notification.dto.NotificationResponseDto;
import Ranaka.ranaka.notification.entity.PushToken;
import Ranaka.ranaka.notification.repository.PushTokenRepository;
import Ranaka.ranaka.notification.service.PushNotificationDeliveryService;
import Ranaka.ranaka.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpoPushNotificationDeliveryService implements PushNotificationDeliveryService {

    private final PushTokenRepository pushTokenRepository;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${expo.push.url:https://exp.host/--/api/v2/push/send}")
    private String expoPushUrl;

    @Value("${expo.push.access-token:}")
    private String expoPushAccessToken;

    @Override
    public void deliverNotification(User recipient, NotificationResponseDto notification) {
        List<PushToken> activeTokens = pushTokenRepository.findByUserIdAndActiveTrue(recipient.getId());
        if (activeTokens.isEmpty()) {
            return;
        }

        try {
            List<Map<String, Object>> payload = buildPayload(activeTokens, notification);
            HttpRequest request = buildRequest(payload);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.warn("Expo push delivery failed with status {} for user {}: {}",
                        response.statusCode(),
                        recipient.getEmail(),
                        response.body());
                return;
            }

            deactivateInvalidTokens(activeTokens, response.body());
        } catch (Exception ex) {
            log.warn("Failed to deliver push notification to {}: {}", recipient.getEmail(), ex.getMessage());
        }
    }

    private List<Map<String, Object>> buildPayload(List<PushToken> activeTokens, NotificationResponseDto notification) {
        List<Map<String, Object>> payload = new ArrayList<>();

        for (PushToken pushToken : activeTokens) {
            Map<String, Object> message = new LinkedHashMap<>();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("notificationId", notification.getId());
            data.put("referenceId", notification.getReferenceId());
            data.put("referenceType", notification.getReferenceType());
            data.put("type", notification.getType().name());

            message.put("to", pushToken.getToken());
            message.put("title", notification.getTitle());
            message.put("body", notification.getMessage());
            message.put("sound", "default");
            message.put("priority", "high");
            message.put("channelId", "ranaka-updates");
            message.put("data", data);
            payload.add(message);
        }

        return payload;
    }

    private HttpRequest buildRequest(List<Map<String, Object>> payload) throws IOException {
        String requestBody = objectMapper.writeValueAsString(payload);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(expoPushUrl))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        if (StringUtils.hasText(expoPushAccessToken)) {
            builder.header("Authorization", "Bearer " + expoPushAccessToken.trim());
        }

        return builder.build();
    }

    private void deactivateInvalidTokens(List<PushToken> pushTokens, String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                return;
            }

            for (int index = 0; index < data.size() && index < pushTokens.size(); index++) {
                JsonNode ticket = data.get(index);
                if (!"error".equalsIgnoreCase(ticket.path("status").asText())) {
                    continue;
                }

                String errorCode = ticket.path("details").path("error").asText();
                if (!"DeviceNotRegistered".equalsIgnoreCase(errorCode)) {
                    continue;
                }

                PushToken pushToken = pushTokens.get(index);
                pushToken.setActive(false);
                pushTokenRepository.save(pushToken);
            }
        } catch (Exception ex) {
            log.warn("Could not inspect Expo push response: {}", ex.getMessage());
        }
    }
}
