package mate.academy.carsharingservice.dto;

import jakarta.validation.constraints.NotNull;
import mate.academy.carsharingservice.model.PaymentType;

public record CreatePaymentRequestDto(
        @NotNull(message = "Rental ID must not be null")
        Long rentalId,
        @NotNull(message = "Payment type must not be null")
        PaymentType type
) {
}
