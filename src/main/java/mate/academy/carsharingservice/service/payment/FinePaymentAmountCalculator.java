package mate.academy.carsharingservice.service.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import mate.academy.carsharingservice.model.PaymentType;
import mate.academy.carsharingservice.model.Rental;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FinePaymentAmountCalculator implements PaymentAmountCalculator {
    private final BigDecimal fineMultiplier;

    public FinePaymentAmountCalculator(
            @Value("${payment.fine-multiplier}") BigDecimal fineMultiplier) {
        this.fineMultiplier = fineMultiplier;
    }

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.FINE;
    }

    @Override
    public BigDecimal calculate(Rental rental) {
        long overdueDays = ChronoUnit.DAYS.between(
                rental.getReturnDate(), rental.getActualReturnDate());
        return rental.getCar().getDailyFee()
                .multiply(BigDecimal.valueOf(overdueDays))
                .multiply(fineMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
