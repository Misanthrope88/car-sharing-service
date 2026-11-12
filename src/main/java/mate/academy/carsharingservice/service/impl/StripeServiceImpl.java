package mate.academy.carsharingservice.service.impl;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import mate.academy.carsharingservice.dto.StripeSessionDto;
import mate.academy.carsharingservice.exception.PaymentGatewayException;
import mate.academy.carsharingservice.service.StripeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeServiceImpl implements StripeService {
    private static final long ONE_CAR_QUANTITY = 1L;
    private static final int CENTS_SCALE = 2;

    private final StripeClient stripeClient;
    private final String currency;

    public StripeServiceImpl(
            @Value("${stripe.secret-key}") String secretKey,
            @Value("${stripe.currency}") String currency) {
        this.stripeClient = new StripeClient(secretKey);
        this.currency = currency;
    }

    @Override
    public StripeSessionDto createSession(
            BigDecimal amount, String productName, String successUrl, String cancelUrl) {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(ONE_CAR_QUANTITY)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency)
                                .setUnitAmount(amount.movePointRight(CENTS_SCALE)
                                        .longValueExact())
                                .setProductData(SessionCreateParams.LineItem.PriceData
                                        .ProductData.builder()
                                        .setName(productName)
                                        .build())
                                .build())
                        .build())
                .build();
        try {
            Session session = stripeClient.v1().checkout().sessions().create(params);
            return new StripeSessionDto(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new PaymentGatewayException("Can't create payment session", e);
        }
    }

    @Override
    public boolean isSessionPaid(String sessionId) {
        try {
            Session session = stripeClient.v1().checkout().sessions().retrieve(sessionId);
            return "paid".equals(session.getPaymentStatus());
        } catch (StripeException e) {
            throw new PaymentGatewayException("Can't retrieve payment session: " + sessionId, e);
        }
    }
}
