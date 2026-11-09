package mate.academy.carsharingservice.dto;

public record StripeSessionDto(
        String sessionId,
        String sessionUrl
) {
}
