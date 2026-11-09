package mate.academy.carsharingservice.service;

import java.math.BigDecimal;
import mate.academy.carsharingservice.dto.StripeSessionDto;

public interface StripeService {
    StripeSessionDto createSession(
            BigDecimal amount, String productName, String successUrl, String cancelUrl);

    boolean isSessionPaid(String sessionId);
}
