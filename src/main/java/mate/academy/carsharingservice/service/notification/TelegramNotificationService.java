package mate.academy.carsharingservice.service.notification;

import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
public class TelegramNotificationService implements NotificationService {
    private final boolean enabled;
    private final String botToken;
    private final String chatId;
    private final RestClient restClient;

    public TelegramNotificationService(
            @Value("${telegram.enabled}") boolean enabled,
            @Value("${telegram.api-url}") String apiUrl,
            @Value("${telegram.bot-token}") String botToken,
            @Value("${telegram.chat-id}") String chatId) {
        this.enabled = enabled;
        this.botToken = botToken;
        this.chatId = chatId;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public void sendMessage(String message) {
        if (!enabled) {
            log.debug("Telegram notifications are disabled; message not sent");
            return;
        }
        if (message == null || message.isBlank()) {
            log.warn("Telegram notification skipped: message is blank");
            return;
        }
        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            log.warn("Telegram notification skipped: bot token or chat id is not configured");
            return;
        }
        try {
            restClient.post()
                    .uri("/bot{token}/sendMessage", botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", chatId, "text", message))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Failed to send Telegram notification: {}", e.getClass().getSimpleName());
        }
    }
}
