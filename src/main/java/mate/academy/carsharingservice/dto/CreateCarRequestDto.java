package mate.academy.carsharingservice.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import mate.academy.carsharingservice.model.CarType;

public record CreateCarRequestDto(
        @NotBlank(message = "Model must not be blank")
        @Size(max = 255, message = "Model must not exceed 255 characters")
        String model,
        @NotBlank(message = "Brand must not be blank")
        @Size(max = 255, message = "Brand must not exceed 255 characters")
        String brand,
        @NotNull(message = "Type must not be null")
        CarType type,
        @NotNull(message = "Inventory must not be null")
        @Min(value = 0, message = "Inventory must not be negative")
        Integer inventory,
        @NotNull(message = "Daily fee must not be null")
        @Positive(message = "Daily fee must be greater than zero")
        @Digits(
                integer = 17,
                fraction = 2,
                message = "Daily fee must have up to 17 integer digits and 2 decimal places"
        )
        BigDecimal dailyFee
) {
}
