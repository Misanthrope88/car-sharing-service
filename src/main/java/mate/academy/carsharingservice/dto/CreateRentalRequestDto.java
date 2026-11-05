package mate.academy.carsharingservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateRentalRequestDto(
        @NotNull(message = "Car ID must not be null")
        Long carId,
        @NotNull(message = "Rental date must not be null")
        LocalDate rentalDate,
        @NotNull(message = "Return date must not be null")
        LocalDate returnDate
) {
    @AssertTrue(message = "Return date must not be earlier than rental date")
    @JsonIgnore
    public boolean isDateRangeValid() {
        return rentalDate == null || returnDate == null || !returnDate.isBefore(rentalDate);
    }
}
