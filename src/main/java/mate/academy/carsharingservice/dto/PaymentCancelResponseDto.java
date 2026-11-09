package mate.academy.carsharingservice.dto;

public record PaymentCancelResponseDto(
        String message,
        String sessionUrl
) {
}
