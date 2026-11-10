package mate.academy.carsharingservice.service.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TelegramNotificationServiceTest {
    private static final String API_URL = "http://localhost:1";
    private static final String BOT_TOKEN = "test-bot-token";
    private static final String CHAT_ID = "123456";
    private static final String MESSAGE = "Test message";

    @Test
    @DisplayName("Disabled notifications return without sending")
    void sendMessage_Disabled_ReturnsWithoutException() {
        // Given
        TelegramNotificationService service = createService(false, BOT_TOKEN, CHAT_ID);

        // When / Then
        assertDoesNotThrow(() -> service.sendMessage(MESSAGE));
    }

    @Test
    @DisplayName("Enabled notifications with a blank bot token return without sending")
    void sendMessage_BlankBotToken_ReturnsWithoutException() {
        // Given
        TelegramNotificationService service = createService(true, " ", CHAT_ID);

        // When / Then
        assertDoesNotThrow(() -> service.sendMessage(MESSAGE));
    }

    @Test
    @DisplayName("Enabled notifications with a blank chat id return without sending")
    void sendMessage_BlankChatId_ReturnsWithoutException() {
        // Given
        TelegramNotificationService service = createService(true, BOT_TOKEN, "");

        // When / Then
        assertDoesNotThrow(() -> service.sendMessage(MESSAGE));
    }

    private TelegramNotificationService createService(
            boolean enabled, String botToken, String chatId) {
        return new TelegramNotificationService(enabled, API_URL, botToken, chatId);
    }
}
