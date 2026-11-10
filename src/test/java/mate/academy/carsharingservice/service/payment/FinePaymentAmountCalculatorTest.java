package mate.academy.carsharingservice.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import mate.academy.carsharingservice.model.Car;
import mate.academy.carsharingservice.model.PaymentType;
import mate.academy.carsharingservice.model.Rental;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FinePaymentAmountCalculatorTest {
    private static final BigDecimal FINE_MULTIPLIER = new BigDecimal("1.5");
    private static final BigDecimal DAILY_FEE = new BigDecimal("10.00");
    private static final LocalDate RETURN_DATE = LocalDate.of(2026, 1, 10);

    private final FinePaymentAmountCalculator calculator =
            new FinePaymentAmountCalculator(FINE_MULTIPLIER);

    @Test
    @DisplayName("Two overdue days at 10.00 with multiplier 1.5 cost 30.00")
    void calculate_TwoOverdueDays_ReturnsFeeTimesDaysTimesMultiplier() {
        // Given
        Rental rental = createRental(RETURN_DATE, RETURN_DATE.plusDays(2));

        // When
        BigDecimal actual = calculator.calculate(rental);

        // Then
        assertEquals(new BigDecimal("30.00"), actual);
        assertEquals(2, actual.scale());
    }

    @Test
    @DisplayName("Calculator handles the FINE type")
    void getPaymentType_Always_ReturnsFine() {
        // When
        PaymentType actual = calculator.getPaymentType();

        // Then
        assertEquals(PaymentType.FINE, actual);
    }

    private Rental createRental(LocalDate returnDate, LocalDate actualReturnDate) {
        Car car = new Car();
        car.setDailyFee(DAILY_FEE);
        Rental rental = new Rental();
        rental.setRentalDate(returnDate.minusDays(3));
        rental.setReturnDate(returnDate);
        rental.setActualReturnDate(actualReturnDate);
        rental.setCar(car);
        return rental;
    }
}
