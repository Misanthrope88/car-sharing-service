package mate.academy.carsharingservice.dto;

import java.math.BigDecimal;
import mate.academy.carsharingservice.model.CarType;

public record CarDto(
        Long id,
        String model,
        String brand,
        CarType type,
        int inventory,
        BigDecimal dailyFee
) {
}
