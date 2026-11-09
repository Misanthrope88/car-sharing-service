package mate.academy.carsharingservice.dto;

import java.math.BigDecimal;
import mate.academy.carsharingservice.model.PaymentStatus;
import mate.academy.carsharingservice.model.PaymentType;

public record PaymentDto(
        Long id,
        PaymentStatus status,
        PaymentType type,
        Long rentalId,
        String sessionUrl,
        String sessionId,
        BigDecimal amountToPay
) {
}
