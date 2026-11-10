package mate.academy.carsharingservice.service.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import mate.academy.carsharingservice.model.Car;
import mate.academy.carsharingservice.model.CarType;
import mate.academy.carsharingservice.model.Rental;
import mate.academy.carsharingservice.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationMessageBuilderTest {
    private static final Long RENTAL_ID = 42L;
    private static final String EMAIL = "john.doe@example.com";
    private static final String BRAND = "Toyota";
    private static final String MODEL = "Corolla";
    private static final BigDecimal DAILY_FEE = new BigDecimal("10.00");

    private final NotificationMessageBuilder messageBuilder = new NotificationMessageBuilder();

    @Test
    @DisplayName("New rental message contains rental, customer, car and dates")
    void buildNewRentalMessage_ValidRental_ContainsRentalDetails() {
        // Given
        Rental rental = createRental(LocalDate.now(), LocalDate.now().plusDays(3));

        // When
        String actual = messageBuilder.buildNewRentalMessage(rental);

        // Then
        assertTrue(actual.contains("Rental ID: " + RENTAL_ID));
        assertTrue(actual.contains(EMAIL));
        assertTrue(actual.contains(BRAND));
        assertTrue(actual.contains(MODEL));
        assertTrue(actual.contains("Rental date: " + rental.getRentalDate()));
        assertTrue(actual.contains("Return date: " + rental.getReturnDate()));
    }

    @Test
    @DisplayName("Overdue message contains rental, customer, car, dates and days overdue")
    void buildOverdueRentalMessage_OverdueRental_ContainsRentalDetails() {
        // Given
        Rental rental = createRental(LocalDate.now().minusDays(5), LocalDate.now().minusDays(2));

        // When
        String actual = messageBuilder.buildOverdueRentalMessage(rental);

        // Then
        assertTrue(actual.contains("Rental ID: " + RENTAL_ID));
        assertTrue(actual.contains(EMAIL));
        assertTrue(actual.contains(BRAND));
        assertTrue(actual.contains(MODEL));
        assertTrue(actual.contains("Rental date: " + rental.getRentalDate()));
        assertTrue(actual.contains("Return date: " + rental.getReturnDate()));
        assertTrue(actual.contains("Days overdue: 2"));
    }

    @Test
    @DisplayName("Days overdue is zero when the return date is in the future")
    void buildOverdueRentalMessage_ReturnDateInFuture_DaysOverdueIsZero() {
        // Given
        Rental rental = createRental(LocalDate.now(), LocalDate.now().plusDays(1));

        // When
        String actual = messageBuilder.buildOverdueRentalMessage(rental);

        // Then
        assertTrue(actual.contains("Days overdue: 0"));
    }

    @Test
    @DisplayName("No-overdue message has the expected text")
    void buildNoOverdueRentalsMessage_Always_ReturnsFixedText() {
        // When
        String actual = messageBuilder.buildNoOverdueRentalsMessage();

        // Then
        assertEquals("No rentals overdue today!", actual);
    }

    private Rental createRental(LocalDate rentalDate, LocalDate returnDate) {
        User user = new User();
        user.setEmail(EMAIL);
        user.setFirstName("John");
        user.setLastName("Doe");
        Car car = new Car();
        car.setBrand(BRAND);
        car.setModel(MODEL);
        car.setType(CarType.SEDAN);
        car.setDailyFee(DAILY_FEE);
        Rental rental = new Rental();
        rental.setId(RENTAL_ID);
        rental.setRentalDate(rentalDate);
        rental.setReturnDate(returnDate);
        rental.setUser(user);
        rental.setCar(car);
        return rental;
    }
}
