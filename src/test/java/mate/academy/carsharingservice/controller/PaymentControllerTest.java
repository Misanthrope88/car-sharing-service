package mate.academy.carsharingservice.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mate.academy.carsharingservice.config.AbstractIntegrationTest;
import mate.academy.carsharingservice.dto.StripeSessionDto;
import mate.academy.carsharingservice.exception.PaymentGatewayException;
import mate.academy.carsharingservice.service.StripeService;
import mate.academy.carsharingservice.service.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;

@Sql(scripts = "classpath:database/controller/add-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:database/controller/clear-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PaymentControllerTest extends AbstractIntegrationTest {
    private static final String MANAGER_EMAIL = "manager@carsharing.com";
    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String PAYMENTS_URL = "/payments";
    private static final String SUCCESS_URL = "/payments/success";
    private static final String CANCEL_URL = "/payments/cancel";
    private static final String SESSION_ID_PARAM = "session_id";
    private static final int ANOTHER_CUSTOMER_ID = 102;
    private static final int PENDING_PAYMENT_ID = 101;
    private static final int PAID_PAYMENT_ID = 102;
    private static final int ACTIVE_RENTAL_ID = 101;
    private static final int RETURNED_LATE_RENTAL_ID = 102;
    private static final int RENTAL_WITHOUT_PAYMENTS_ID = 103;
    private static final int FOREIGN_RENTAL_ID = 104;
    private static final String PENDING_SESSION_ID = "cs_test_101";
    private static final String PENDING_SESSION_URL =
            "https://checkout.stripe.com/c/pay/cs_test_101";
    private static final String UNKNOWN_SESSION_ID = "unknown";
    private static final String NEW_SESSION_ID = "cs_test_new";
    private static final String NEW_SESSION_URL = "https://checkout.stripe.com/c/pay/cs_test_new";

    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private StripeService stripeService;

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer sees only own payments")
    void getAll_Customer_ReturnsOwnPayments() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(PAYMENTS_URL));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id",
                        containsInAnyOrder(PENDING_PAYMENT_ID, PAID_PAYMENT_ID)));
    }

    @Test
    @WithUserDetails(value = MANAGER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Manager sees all payments")
    void getAll_Manager_ReturnsAllPayments() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(PAYMENTS_URL));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer filtering payments by user id is forbidden")
    void getAll_CustomerWithUserId_ReturnsForbidden() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(PAYMENTS_URL)
                .param("user_id", String.valueOf(ANOTHER_CUSTOMER_ID)));

        // Then
        result.andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer creates a payment session for a rental without payments")
    void createPaymentSession_NewPayment_ReturnsPendingPayment() throws Exception {
        // Given
        stubStripeSession();

        // When
        ResultActions result = performCreatePayment(RENTAL_WITHOUT_PAYMENTS_ID, "PAYMENT");

        // Then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.type").value("PAYMENT"))
                .andExpect(jsonPath("$.rentalId").value(RENTAL_WITHOUT_PAYMENTS_ID))
                .andExpect(jsonPath("$.sessionId").value(NEW_SESSION_ID))
                .andExpect(jsonPath("$.sessionUrl").value(NEW_SESSION_URL))
                .andExpect(jsonPath("$.amountToPay").value(80.0));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Fine for a rental that is not returned yet returns 409")
    void createPaymentSession_FineForActiveRental_ReturnsConflict() throws Exception {
        // When
        ResultActions result = performCreatePayment(ACTIVE_RENTAL_ID, "FINE");

        // Then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "Fine is available only for rentals returned after the return date"));
        verify(stripeService, never()).createSession(any(), anyString(), anyString(),
                anyString());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Paying for a rental that is already paid returns 409")
    void createPaymentSession_AlreadyPaid_ReturnsConflict() throws Exception {
        // When
        ResultActions result = performCreatePayment(RETURNED_LATE_RENTAL_ID, "PAYMENT");

        // Then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Payment is already completed"));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Existing pending session is reused without calling Stripe")
    void createPaymentSession_ExistingPendingPayment_ReturnsExistingSession() throws Exception {
        // When
        ResultActions result = performCreatePayment(ACTIVE_RENTAL_ID, "PAYMENT");

        // Then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PENDING_PAYMENT_ID))
                .andExpect(jsonPath("$.sessionId").value(PENDING_SESSION_ID));
        verify(stripeService, never()).createSession(any(), anyString(), anyString(),
                anyString());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Fine for a rental returned late is calculated with the multiplier")
    void createPaymentSession_FineForLateReturn_ReturnsFineAmount() throws Exception {
        // Given
        stubStripeSession();

        // When
        ResultActions result = performCreatePayment(RETURNED_LATE_RENTAL_ID, "FINE");

        // Then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.type").value("FINE"))
                .andExpect(jsonPath("$.amountToPay").value(120.0));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer paying for a rental of another user is forbidden")
    void createPaymentSession_ForeignRental_ReturnsForbidden() throws Exception {
        // When
        ResultActions result = performCreatePayment(FOREIGN_RENTAL_ID, "PAYMENT");

        // Then
        result.andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Stripe failure returns 502 with a generic message")
    void createPaymentSession_StripeFails_ReturnsBadGateway() throws Exception {
        // Given
        when(stripeService.createSession(any(), anyString(), anyString(), anyString()))
                .thenThrow(new PaymentGatewayException("x", new RuntimeException()));

        // When
        ResultActions result = performCreatePayment(RENTAL_WITHOUT_PAYMENTS_ID, "PAYMENT");

        // Then
        result.andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Payment provider error"));
    }

    @Test
    @DisplayName("Success redirect for a paid session marks the payment as PAID")
    void handleSuccess_SessionPaid_ReturnsPaidPayment() throws Exception {
        // Given
        when(stripeService.isSessionPaid(PENDING_SESSION_ID)).thenReturn(true);

        // When
        ResultActions result = mockMvc.perform(get(SUCCESS_URL)
                .param(SESSION_ID_PARAM, PENDING_SESSION_ID));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PENDING_PAYMENT_ID))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("Success redirect for an unpaid session returns 409")
    void handleSuccess_SessionNotPaid_ReturnsConflict() throws Exception {
        // Given
        when(stripeService.isSessionPaid(PENDING_SESSION_ID)).thenReturn(false);

        // When
        ResultActions result = mockMvc.perform(get(SUCCESS_URL)
                .param(SESSION_ID_PARAM, PENDING_SESSION_ID));

        // Then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Payment is not completed"));
    }

    @Test
    @DisplayName("Success redirect for an unknown session returns 404")
    void handleSuccess_UnknownSession_ReturnsNotFound() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(SUCCESS_URL)
                .param(SESSION_ID_PARAM, UNKNOWN_SESSION_ID));

        // Then
        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Cancel redirect returns a message and the session URL")
    void handleCancel_ExistingSession_ReturnsSessionUrl() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(CANCEL_URL)
                .param(SESSION_ID_PARAM, PENDING_SESSION_ID));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.sessionUrl").value(PENDING_SESSION_URL));
    }

    private void stubStripeSession() {
        when(stripeService.createSession(any(), anyString(), anyString(), anyString()))
                .thenReturn(new StripeSessionDto(NEW_SESSION_ID, NEW_SESSION_URL));
    }

    private ResultActions performCreatePayment(int rentalId, String type) throws Exception {
        String body = """
                {
                  "rentalId": %d,
                  "type": "%s"
                }
                """.formatted(rentalId, type);
        return mockMvc.perform(post(PAYMENTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
