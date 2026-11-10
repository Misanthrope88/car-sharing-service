package mate.academy.carsharingservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import mate.academy.carsharingservice.model.Payment;
import mate.academy.carsharingservice.model.PaymentStatus;
import mate.academy.carsharingservice.model.PaymentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:database/repository/add-repository-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:database/repository/clear-repository-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PaymentRepositoryTest extends AbstractRepositoryTest {
    private static final Long CUSTOMER_ID = 101L;
    private static final Long ANOTHER_CUSTOMER_ID = 102L;
    private static final Long RENTAL_ID = 101L;
    private static final Long PENDING_PAYMENT_ID = 101L;
    private static final Long PAID_FINE_ID = 102L;
    private static final Long ANOTHER_CUSTOMER_PAYMENT_ID = 103L;
    private static final String PAID_SESSION_ID = "cs_test_102";
    private static final String UNKNOWN_SESSION_ID = "cs_test_unknown";

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("Payments of the first customer are returned")
    void findAllByRentalUserId_CustomerWithTwoPayments_ReturnsBothPayments() {
        // When
        Page<Payment> actual = paymentRepository.findAllByRentalUserId(CUSTOMER_ID, sortedPage());

        // Then
        assertEquals(List.of(PENDING_PAYMENT_ID, PAID_FINE_ID), extractIds(actual));
    }

    @Test
    @DisplayName("Payments of the second customer are returned")
    void findAllByRentalUserId_CustomerWithOnePayment_ReturnsOnePayment() {
        // When
        Page<Payment> actual =
                paymentRepository.findAllByRentalUserId(ANOTHER_CUSTOMER_ID, sortedPage());

        // Then
        assertEquals(List.of(ANOTHER_CUSTOMER_PAYMENT_ID), extractIds(actual));
    }

    @Test
    @DisplayName("Find a payment by an existing session id")
    void findBySessionId_ExistingSession_ReturnsPaidFine() {
        // When
        Optional<Payment> actual = paymentRepository.findBySessionId(PAID_SESSION_ID);

        // Then
        assertTrue(actual.isPresent());
        assertEquals(PAID_FINE_ID, actual.get().getId());
        assertEquals(PaymentStatus.PAID, actual.get().getStatus());
        assertEquals(PaymentType.FINE, actual.get().getType());
    }

    @Test
    @DisplayName("Find a payment by an unknown session id returns empty")
    void findBySessionId_UnknownSession_ReturnsEmpty() {
        // When
        Optional<Payment> actual = paymentRepository.findBySessionId(UNKNOWN_SESSION_ID);

        // Then
        assertTrue(actual.isEmpty());
    }

    @Test
    @DisplayName("Find a payment by rental id and existing type")
    void findByRentalIdAndType_ExistingType_ReturnsPayment() {
        // When
        Optional<Payment> actual =
                paymentRepository.findByRentalIdAndType(RENTAL_ID, PaymentType.PAYMENT);

        // Then
        assertTrue(actual.isPresent());
        assertEquals(PENDING_PAYMENT_ID, actual.get().getId());
    }

    @Test
    @DisplayName("Find a payment by rental id and missing type returns empty")
    void findByRentalIdAndType_MissingType_ReturnsEmpty() {
        // When
        Optional<Payment> actual =
                paymentRepository.findByRentalIdAndType(RENTAL_ID, PaymentType.FINE);

        // Then
        assertTrue(actual.isEmpty());
    }

    private PageRequest sortedPage() {
        return PageRequest.of(0, 10, Sort.by("id"));
    }

    private List<Long> extractIds(Page<Payment> payments) {
        return payments.getContent().stream()
                .map(Payment::getId)
                .toList();
    }
}
