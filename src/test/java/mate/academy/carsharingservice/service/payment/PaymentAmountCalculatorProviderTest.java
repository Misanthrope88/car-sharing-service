package mate.academy.carsharingservice.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import mate.academy.carsharingservice.model.PaymentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentAmountCalculatorProviderTest {
    private static final BigDecimal FINE_MULTIPLIER = new BigDecimal("1.5");

    private final RentalPaymentAmountCalculator rentalCalculator =
            new RentalPaymentAmountCalculator();
    private final FinePaymentAmountCalculator fineCalculator =
            new FinePaymentAmountCalculator(FINE_MULTIPLIER);

    @Test
    @DisplayName("PAYMENT type resolves to the rental calculator")
    void getCalculator_PaymentType_ReturnsRentalCalculator() {
        // Given
        PaymentAmountCalculatorProvider provider = createProvider(
                List.of(rentalCalculator, fineCalculator));

        // When
        PaymentAmountCalculator actual = provider.getCalculator(PaymentType.PAYMENT);

        // Then
        assertSame(rentalCalculator, actual);
    }

    @Test
    @DisplayName("FINE type resolves to the fine calculator")
    void getCalculator_FineType_ReturnsFineCalculator() {
        // Given
        PaymentAmountCalculatorProvider provider = createProvider(
                List.of(rentalCalculator, fineCalculator));

        // When
        PaymentAmountCalculator actual = provider.getCalculator(PaymentType.FINE);

        // Then
        assertSame(fineCalculator, actual);
    }

    @Test
    @DisplayName("Type without a registered calculator throws an exception")
    void getCalculator_MissingCalculator_ThrowsIllegalArgumentException() {
        // Given
        PaymentAmountCalculatorProvider provider = createProvider(List.of(rentalCalculator));

        // When
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> provider.getCalculator(PaymentType.FINE));

        // Then
        assertEquals("No payment amount calculator for type: FINE", exception.getMessage());
    }

    private PaymentAmountCalculatorProvider createProvider(
            List<PaymentAmountCalculator> calculators) {
        return new PaymentAmountCalculatorProvider(calculators);
    }
}
