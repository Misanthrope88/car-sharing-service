package mate.academy.carsharingservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateRentalRequestDto(
        @NotNull(message = "Car ID must not be null")
        Long carId,
        @NotNull(message = "Return date must not be null")
        @Future(message = "Return date must be in the future")
        LocalDate returnDate
) {
}
