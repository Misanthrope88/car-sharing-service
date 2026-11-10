package mate.academy.carsharingservice.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mate.academy.carsharingservice.dto.CreatePaymentRequestDto;
import mate.academy.carsharingservice.dto.PaymentCancelResponseDto;
import mate.academy.carsharingservice.dto.PaymentDto;
import mate.academy.carsharingservice.dto.StripeSessionDto;
import mate.academy.carsharingservice.exception.EntityNotFoundException;
import mate.academy.carsharingservice.exception.PaymentException;
import mate.academy.carsharingservice.mapper.PaymentMapper;
import mate.academy.carsharingservice.model.Payment;
import mate.academy.carsharingservice.model.PaymentStatus;
import mate.academy.carsharingservice.model.PaymentType;
import mate.academy.carsharingservice.model.Rental;
import mate.academy.carsharingservice.model.Role;
import mate.academy.carsharingservice.model.User;
import mate.academy.carsharingservice.repository.PaymentRepository;
import mate.academy.carsharingservice.repository.RentalRepository;
import mate.academy.carsharingservice.service.StripeService;
import mate.academy.carsharingservice.service.payment.PaymentAmountCalculator;
import mate.academy.carsharingservice.service.payment.PaymentAmountCalculatorProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    private static final Long CUSTOMER_ID = 1L;
    private static final Long OTHER_CUSTOMER_ID = 2L;
    private static final Long MANAGER_ID = 3L;
    private static final Long RENTAL_ID = 10L;
    private static final Long PAYMENT_ID = 20L;
    private static final Long NON_EXISTING_ID = 100L;
    private static final BigDecimal AMOUNT = new BigDecimal("30.00");
    private static final String SESSION_ID = "cs_test_123";
    private static final String NON_EXISTING_SESSION_ID = "cs_test_missing";
    private static final String SESSION_URL = "https://checkout.stripe.com/c/pay/cs_test_123";
    private static final String NEW_SESSION_ID = "cs_test_new";
    private static final String NEW_SESSION_URL = "https://checkout.stripe.com/c/pay/cs_test_new";
    private static final String SESSION_PLACEHOLDER = "{CHECKOUT_SESSION_ID}";
    private static final String CANCEL_MESSAGE = "Payment was canceled. You can complete it "
            + "later, but the session is available for 24 hours.";

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private StripeService stripeService;
    @Mock
    private PaymentAmountCalculatorProvider calculatorProvider;
    @Mock
    private PaymentAmountCalculator calculator;
    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Customer filtering payments by user id is denied")
    void getAll_CustomerWithUserId_ThrowsAccessDeniedException() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        assertThrows(AccessDeniedException.class,
                () -> paymentService.getAll(customer, OTHER_CUSTOMER_ID, pageable));

        // Then
        verify(paymentRepository, never()).findAllByRentalUserId(any(), any());
    }

    @Test
    @DisplayName("Customer gets only own payments")
    void getAll_CustomerWithoutUserId_ReturnsOwnPayments() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Pageable pageable = PageRequest.of(0, 10);
        Payment payment = createPayment(PaymentStatus.PENDING, PaymentType.PAYMENT);
        PaymentDto dto = createPaymentDto(PaymentStatus.PENDING);
        when(paymentRepository.findAllByRentalUserId(CUSTOMER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(payment), pageable, 1));
        when(paymentMapper.toDto(payment)).thenReturn(dto);

        // When
        Page<PaymentDto> actual = paymentService.getAll(customer, null, pageable);

        // Then
        assertEquals(List.of(dto), actual.getContent());
        verify(paymentRepository).findAllByRentalUserId(CUSTOMER_ID, pageable);
    }

    @Test
    @DisplayName("Manager without user filter gets all payments")
    void getAll_ManagerWithoutUserId_ReturnsAllPayments() {
        // Given
        User manager = createUser(MANAGER_ID, Role.MANAGER);
        Pageable pageable = PageRequest.of(0, 10);
        Payment payment = createPayment(PaymentStatus.PENDING, PaymentType.PAYMENT);
        PaymentDto dto = createPaymentDto(PaymentStatus.PENDING);
        when(paymentRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(payment), pageable, 1));
        when(paymentMapper.toDto(payment)).thenReturn(dto);

        // When
        Page<PaymentDto> actual = paymentService.getAll(manager, null, pageable);

        // Then
        assertEquals(List.of(dto), actual.getContent());
        verify(paymentRepository, never()).findAllByRentalUserId(any(), any());
    }

    @Test
    @DisplayName("Manager with user filter gets payments of that user")
    void getAll_ManagerWithUserId_ReturnsPaymentsOfUser() {
        // Given
        User manager = createUser(MANAGER_ID, Role.MANAGER);
        Pageable pageable = PageRequest.of(0, 10);
        Payment payment = createPayment(PaymentStatus.PAID, PaymentType.PAYMENT);
        PaymentDto dto = createPaymentDto(PaymentStatus.PAID);
        when(paymentRepository.findAllByRentalUserId(CUSTOMER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(payment), pageable, 1));
        when(paymentMapper.toDto(payment)).thenReturn(dto);

        // When
        Page<PaymentDto> actual = paymentService.getAll(manager, CUSTOMER_ID, pageable);

        // Then
        assertEquals(List.of(dto), actual.getContent());
        verify(paymentRepository).findAllByRentalUserId(CUSTOMER_ID, pageable);
    }

    @Test
    @DisplayName("Create a payment session for a non-existing rental throws an exception")
    void createPaymentSession_NonExistingRental_ThrowsEntityNotFoundException() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        CreatePaymentRequestDto request =
                new CreatePaymentRequestDto(NON_EXISTING_ID, PaymentType.PAYMENT);
        when(rentalRepository.findByIdForUpdate(NON_EXISTING_ID)).thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentService.createPaymentSession(request, customer));

        // Then
        assertEquals("Can't find rental by id: " + NON_EXISTING_ID, exception.getMessage());
        verifyStripeNeverCalled();
    }

    @Test
    @DisplayName("Customer creating a payment for a rental of another user is denied")
    void createPaymentSession_CustomerOnForeignRental_ThrowsAccessDeniedException() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Rental rental = createRental(createUser(OTHER_CUSTOMER_ID, Role.CUSTOMER), null);
        CreatePaymentRequestDto request =
                new CreatePaymentRequestDto(RENTAL_ID, PaymentType.PAYMENT);
        when(rentalRepository.findByIdForUpdate(RENTAL_ID)).thenReturn(Optional.of(rental));

        // When
        assertThrows(AccessDeniedException.class,
                () -> paymentService.createPaymentSession(request, customer));

        // Then
        verifyStripeNeverCalled();
    }

    @Test
    @DisplayName("Fine for a rental that is not returned yet is rejected")
    void createPaymentSession_FineForNotReturnedRental_ThrowsPaymentException() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Rental rental = createRental(customer, null);
        CreatePaymentRequestDto request = new CreatePaymentRequestDto(RENTAL_ID, PaymentType.FINE);
        when(rentalRepository.findByIdForUpdate(RENTAL_ID)).thenReturn(Optional.of(rental));

        // When
        PaymentException exception = assertThrows(PaymentException.class,
                () -> paymentService.createPaymentSession(request, customer));

        // Then
        assertEquals("Fine is available only for rentals returned after the return date",
                exception.getMessage());
        verifyStripeNeverCalled();
    }

    @Test
    @DisplayName("Fine for a rental returned on time is rejected")
    void createPaymentSession_FineForRentalReturnedOnTime_ThrowsPaymentException() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Rental rental = createRental(customer, null);
        rental.setActualReturnDate(rental.getReturnDate());
        CreatePaymentRequestDto request = new CreatePaymentRequestDto(RENTAL_ID, PaymentType.FINE);
        when(rentalRepository.findByIdForUpdate(RENTAL_ID)).thenReturn(Optional.of(rental));

        // When
        PaymentException exception = assertThrows(PaymentException.class,
                () -> paymentService.createPaymentSession(request, customer));

        // Then
        assertEquals("Fine is available only for rentals returned after the return date",
                exception.getMessage());
        verifyStripeNeverCalled();
    }

    @Test
    @DisplayName("Creating a payment that is already paid is rejected")
    void createPaymentSession_ExistingPaidPayment_ThrowsPaymentException() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Rental rental = createRental(customer, null);
        Payment existing = createPayment(PaymentStatus.PAID, PaymentType.PAYMENT);
        CreatePaymentRequestDto request =
                new CreatePaymentRequestDto(RENTAL_ID, PaymentType.PAYMENT);
        when(rentalRepository.findByIdForUpdate(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(paymentRepository.findByRentalIdAndType(RENTAL_ID, PaymentType.PAYMENT))
                .thenReturn(Optional.of(existing));

        // When
        PaymentException exception = assertThrows(PaymentException.class,
                () -> paymentService.createPaymentSession(request, customer));

        // Then
        assertEquals("Payment is already completed", exception.getMessage());
        verifyStripeNeverCalled();
    }

    @Test
    @DisplayName("Existing pending payment is reused without creating a new Stripe session")
    void createPaymentSession_ExistingPendingPayment_ReturnsExistingPayment() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Rental rental = createRental(customer, null);
        Payment existing = createPayment(PaymentStatus.PENDING, PaymentType.PAYMENT);
        PaymentDto expected = createPaymentDto(PaymentStatus.PENDING);
        CreatePaymentRequestDto request =
                new CreatePaymentRequestDto(RENTAL_ID, PaymentType.PAYMENT);
        when(rentalRepository.findByIdForUpdate(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(paymentRepository.findByRentalIdAndType(RENTAL_ID, PaymentType.PAYMENT))
                .thenReturn(Optional.of(existing));
        when(paymentMapper.toDto(existing)).thenReturn(expected);

        // When
        PaymentDto actual = paymentService.createPaymentSession(request, customer);

        // Then
        assertEquals(expected, actual);
        verifyStripeNeverCalled();
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Create a payment session: Stripe called with amount and URLs, payment saved")
    void createPaymentSession_NewPayment_CreatesStripeSessionAndSavesPendingPayment() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Rental rental = createRental(customer, null);
        PaymentDto expected = createPaymentDto(PaymentStatus.PENDING);
        CreatePaymentRequestDto request =
                new CreatePaymentRequestDto(RENTAL_ID, PaymentType.PAYMENT);
        when(rentalRepository.findByIdForUpdate(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(paymentRepository.findByRentalIdAndType(RENTAL_ID, PaymentType.PAYMENT))
                .thenReturn(Optional.empty());
        when(calculatorProvider.getCalculator(PaymentType.PAYMENT)).thenReturn(calculator);
        when(calculator.calculate(rental)).thenReturn(AMOUNT);
        when(stripeService.createSession(eq(AMOUNT), anyString(), anyString(), anyString()))
                .thenReturn(new StripeSessionDto(NEW_SESSION_ID, NEW_SESSION_URL));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(expected);

        // When
        PaymentDto actual = paymentService.createPaymentSession(request, customer);

        // Then
        assertEquals(expected, actual);
        ArgumentCaptor<String> successUrlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cancelUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(stripeService).createSession(eq(AMOUNT), eq("PAYMENT for rental #" + RENTAL_ID),
                successUrlCaptor.capture(), cancelUrlCaptor.capture());
        String successUrl = successUrlCaptor.getValue();
        String cancelUrl = cancelUrlCaptor.getValue();
        assertTrue(successUrl.contains("/payments/success"));
        assertTrue(successUrl.contains(SESSION_PLACEHOLDER));
        assertTrue(cancelUrl.contains("/payments/cancel"));
        assertTrue(cancelUrl.contains(SESSION_PLACEHOLDER));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(PaymentStatus.PENDING, savedPayment.getStatus());
        assertEquals(PaymentType.PAYMENT, savedPayment.getType());
        assertEquals(NEW_SESSION_ID, savedPayment.getSessionId());
        assertEquals(NEW_SESSION_URL, savedPayment.getSessionUrl());
        assertEquals(AMOUNT, savedPayment.getAmountToPay());
        assertEquals(rental, savedPayment.getRental());
    }

    @Test
    @DisplayName("Success redirect with an unknown session id throws an exception")
    void handleSuccess_NonExistingSession_ThrowsEntityNotFoundException() {
        // Given
        when(paymentRepository.findBySessionId(NON_EXISTING_SESSION_ID))
                .thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentService.handleSuccess(NON_EXISTING_SESSION_ID));

        // Then
        assertEquals("Can't find payment by session id: " + NON_EXISTING_SESSION_ID,
                exception.getMessage());
    }

    @Test
    @DisplayName("Success redirect for an already paid payment does not call Stripe")
    void handleSuccess_AlreadyPaid_ReturnsDtoWithoutCallingStripe() {
        // Given
        Payment payment = createPayment(PaymentStatus.PAID, PaymentType.PAYMENT);
        PaymentDto expected = createPaymentDto(PaymentStatus.PAID);
        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(payment));
        when(paymentMapper.toDto(payment)).thenReturn(expected);

        // When
        PaymentDto actual = paymentService.handleSuccess(SESSION_ID);

        // Then
        assertEquals(expected, actual);
        verify(stripeService, never()).isSessionPaid(anyString());
    }

    @Test
    @DisplayName("Success redirect when Stripe reports the session as unpaid throws an exception")
    void handleSuccess_SessionNotPaid_ThrowsPaymentException() {
        // Given
        Payment payment = createPayment(PaymentStatus.PENDING, PaymentType.PAYMENT);
        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(payment));
        when(stripeService.isSessionPaid(SESSION_ID)).thenReturn(false);

        // When
        PaymentException exception = assertThrows(PaymentException.class,
                () -> paymentService.handleSuccess(SESSION_ID));

        // Then
        assertEquals("Payment is not completed", exception.getMessage());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    @DisplayName("Success redirect when Stripe reports the session as paid marks payment PAID")
    void handleSuccess_SessionPaid_MarksPaymentAsPaid() {
        // Given
        Payment payment = createPayment(PaymentStatus.PENDING, PaymentType.PAYMENT);
        PaymentDto expected = createPaymentDto(PaymentStatus.PAID);
        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(payment));
        when(stripeService.isSessionPaid(SESSION_ID)).thenReturn(true);
        when(paymentMapper.toDto(payment)).thenReturn(expected);

        // When
        PaymentDto actual = paymentService.handleSuccess(SESSION_ID);

        // Then
        assertEquals(expected, actual);
        assertEquals(PaymentStatus.PAID, payment.getStatus());
    }

    @Test
    @DisplayName("Cancel redirect returns a message and the session URL")
    void handleCancel_ExistingSession_ReturnsMessageAndSessionUrl() {
        // Given
        Payment payment = createPayment(PaymentStatus.PENDING, PaymentType.PAYMENT);
        when(paymentRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(payment));

        // When
        PaymentCancelResponseDto actual = paymentService.handleCancel(SESSION_ID);

        // Then
        assertEquals(CANCEL_MESSAGE, actual.message());
        assertEquals(SESSION_URL, actual.sessionUrl());
    }

    @Test
    @DisplayName("Cancel redirect with an unknown session id throws an exception")
    void handleCancel_NonExistingSession_ThrowsEntityNotFoundException() {
        // Given
        when(paymentRepository.findBySessionId(NON_EXISTING_SESSION_ID))
                .thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentService.handleCancel(NON_EXISTING_SESSION_ID));

        // Then
        assertEquals("Can't find payment by session id: " + NON_EXISTING_SESSION_ID,
                exception.getMessage());
    }

    private void verifyStripeNeverCalled() {
        verify(stripeService, never()).createSession(any(), any(), any(), any());
    }

    private User createUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setRole(role);
        return user;
    }

    private Rental createRental(User user, LocalDate actualReturnDate) {
        Rental rental = new Rental();
        rental.setId(RENTAL_ID);
        rental.setRentalDate(LocalDate.now().minusDays(5));
        rental.setReturnDate(LocalDate.now().minusDays(2));
        rental.setActualReturnDate(actualReturnDate);
        rental.setUser(user);
        return rental;
    }

    private Payment createPayment(PaymentStatus status, PaymentType type) {
        Payment payment = new Payment();
        payment.setId(PAYMENT_ID);
        payment.setStatus(status);
        payment.setType(type);
        payment.setSessionId(SESSION_ID);
        payment.setSessionUrl(SESSION_URL);
        payment.setAmountToPay(AMOUNT);
        return payment;
    }

    private PaymentDto createPaymentDto(PaymentStatus status) {
        return new PaymentDto(PAYMENT_ID, status, PaymentType.PAYMENT, RENTAL_ID,
                SESSION_URL, SESSION_ID, AMOUNT);
    }
}
