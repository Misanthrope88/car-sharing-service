package mate.academy.carsharingservice.dto;

import java.time.LocalDate;

public record RentalResponseDto(
        Long id,
        LocalDate rentalDate,
        LocalDate returnDate,
        LocalDate actualReturnDate,
        Long userId,
        CarDto car
) {
}
