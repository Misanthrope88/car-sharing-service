package mate.academy.carsharingservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mate.academy.carsharingservice.model.Rental;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:database/repository/add-repository-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:database/repository/clear-repository-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RentalRepositoryTest extends AbstractRepositoryTest {
    private static final Long CUSTOMER_ID = 101L;
    private static final Long ACTIVE_OVERDUE_RENTAL_ID = 101L;
    private static final Long RETURNED_RENTAL_ID = 102L;
    private static final Long ACTIVE_NOT_OVERDUE_RENTAL_ID = 103L;
    private static final Long NON_EXISTING_ID = 999L;
    private static final LocalDate THRESHOLD = LocalDate.of(2026, 1, 20);
    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String TOYOTA_BRAND = "Toyota";

    @Autowired
    private RentalRepository rentalRepository;

    @Test
    @DisplayName("Find rental with details initializes car and user")
    void findWithDetailsById_ExistingId_ReturnsRentalWithCarAndUser() {
        // When
        Optional<Rental> actual = rentalRepository.findWithDetailsById(ACTIVE_OVERDUE_RENTAL_ID);

        // Then
        assertTrue(actual.isPresent());
        Rental rental = actual.get();
        assertTrue(Hibernate.isInitialized(rental.getCar()));
        assertTrue(Hibernate.isInitialized(rental.getUser()));
        assertEquals(TOYOTA_BRAND, rental.getCar().getBrand());
        assertEquals(CUSTOMER_EMAIL, rental.getUser().getEmail());
    }

    @Test
    @DisplayName("Lock an existing rental by id")
    void findByIdForUpdate_ExistingId_ReturnsRental() {
        // When
        Optional<Rental> actual = rentalRepository.findByIdForUpdate(RETURNED_RENTAL_ID);

        // Then
        assertTrue(actual.isPresent());
        assertEquals(RETURNED_RENTAL_ID, actual.get().getId());
    }

    @Test
    @DisplayName("Lock a non-existing rental by id returns empty")
    void findByIdForUpdate_NonExistingId_ReturnsEmpty() {
        // When
        Optional<Rental> actual = rentalRepository.findByIdForUpdate(NON_EXISTING_ID);

        // Then
        assertTrue(actual.isEmpty());
    }

    @Test
    @DisplayName("Overdue query returns only active rentals due by the threshold")
    void findAllByActualReturnDateIsNullAndReturnDateLessThanEqual_Threshold_ReturnsOverdue() {
        // When
        List<Rental> actual = rentalRepository
                .findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(THRESHOLD);

        // Then
        assertEquals(List.of(ACTIVE_OVERDUE_RENTAL_ID), extractTestIds(actual));
    }

    @Test
    @DisplayName("Specification by user id returns rentals of that user")
    void findAll_UserIdSpecification_ReturnsRentalsOfUser() {
        // Given
        Specification<Rental> specification = (root, query, builder) ->
                builder.equal(root.get("user").get("id"), CUSTOMER_ID);

        // When
        Page<Rental> actual = rentalRepository.findAll(specification, sortedPage());

        // Then
        assertEquals(List.of(ACTIVE_OVERDUE_RENTAL_ID, RETURNED_RENTAL_ID),
                extractTestIds(actual.getContent()));
    }

    @Test
    @DisplayName("Specification for active rentals returns rentals without actual return date")
    void findAll_ActiveSpecification_ReturnsActiveRentals() {
        // Given
        Specification<Rental> specification = (root, query, builder) ->
                builder.isNull(root.get("actualReturnDate"));

        // When
        Page<Rental> actual = rentalRepository.findAll(specification, sortedPage());

        // Then
        assertEquals(List.of(ACTIVE_OVERDUE_RENTAL_ID, ACTIVE_NOT_OVERDUE_RENTAL_ID),
                extractTestIds(actual.getContent()));
    }

    private PageRequest sortedPage() {
        return PageRequest.of(0, 10, Sort.by("id"));
    }

    private List<Long> extractTestIds(List<Rental> rentals) {
        return rentals.stream()
                .map(Rental::getId)
                .filter(id -> id >= ACTIVE_OVERDUE_RENTAL_ID)
                .sorted()
                .toList();
    }
}
