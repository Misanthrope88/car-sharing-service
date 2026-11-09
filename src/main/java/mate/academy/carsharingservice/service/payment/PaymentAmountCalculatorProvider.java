package mate.academy.carsharingservice.service.payment;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mate.academy.carsharingservice.model.PaymentType;
import org.springframework.stereotype.Component;

@Component
public class PaymentAmountCalculatorProvider {
    private final Map<PaymentType, PaymentAmountCalculator> calculators =
            new EnumMap<>(PaymentType.class);

    public PaymentAmountCalculatorProvider(List<PaymentAmountCalculator> calculators) {
        for (PaymentAmountCalculator calculator : calculators) {
            this.calculators.put(calculator.getPaymentType(), calculator);
        }
    }

    public PaymentAmountCalculator getCalculator(PaymentType type) {
        PaymentAmountCalculator calculator = calculators.get(type);
        if (calculator == null) {
            throw new IllegalArgumentException("No payment amount calculator for type: " + type);
        }
        return calculator;
    }
}
