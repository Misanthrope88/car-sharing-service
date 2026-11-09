package mate.academy.carsharingservice.service.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import mate.academy.carsharingservice.model.PaymentType;
import mate.academy.carsharingservice.model.Rental;
import org.springframework.stereotype.Component;

@Component
public class RentalPaymentAmountCalculator implements PaymentAmountCalculator {
    @Override
    public PaymentType getPaymentType() {
        return PaymentType.PAYMENT;
    }

    @Override
    public BigDecimal calculate(Rental rental) {
        long days = Math.max(1,
                ChronoUnit.DAYS.between(rental.getRentalDate(), rental.getReturnDate()));
        return rental.getCar().getDailyFee()
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
