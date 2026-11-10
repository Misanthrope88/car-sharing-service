package mate.academy.carsharingservice.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import mate.academy.carsharingservice.model.Car;
import mate.academy.carsharingservice.model.PaymentType;
import mate.academy.carsharingservice.model.Rental;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RentalPaymentAmountCalculatorTest {
    private static final BigDecimal DAILY_FEE = new BigDecimal("10.00");
    private static final LocalDate RENTAL_DATE = LocalDate.of(2026, 1, 10);

    private final RentalPaymentAmountCalculator calculator = new RentalPaymentAmountCalculator();

    @Test
    @DisplayName("Three rental days at 10.00 per day cost 30.00")
    void calculate_ThreeDays_ReturnsDailyFeeTimesDays() {
        // Given
        Rental rental = createRental(RENTAL_DATE, RENTAL_DATE.plusDays(3));

        // When
        BigDecimal actual = calculator.calculate(rental);

        // Then
        assertEquals(new BigDecimal("30.00"), actual);
    }

    @Test
    @DisplayName("Same-day rental is charged as one day")
    void calculate_SameDayRental_ChargesOneDay() {
        // Given
        Rental rental = createRental(RENTAL_DATE, RENTAL_DATE);

        // When
        BigDecimal actual = calculator.calculate(rental);

        // Then
        assertEquals(new BigDecimal("10.00"), actual);
    }

    @Test
    @DisplayName("Calculator handles the PAYMENT type")
    void getPaymentType_Always_ReturnsPayment() {
        // When
        PaymentType actual = calculator.getPaymentType();

        // Then
        assertEquals(PaymentType.PAYMENT, actual);
    }

    private Rental createRental(LocalDate rentalDate, LocalDate returnDate) {
        Car car = new Car();
        car.setDailyFee(DAILY_FEE);
        Rental rental = new Rental();
        rental.setRentalDate(rentalDate);
        rental.setReturnDate(returnDate);
        rental.setCar(car);
        return rental;
    }
}
