package mate.academy.carsharingservice.service.payment;

import java.math.BigDecimal;
import mate.academy.carsharingservice.model.PaymentType;
import mate.academy.carsharingservice.model.Rental;

public interface PaymentAmountCalculator {
    PaymentType getPaymentType();

    BigDecimal calculate(Rental rental);
}
