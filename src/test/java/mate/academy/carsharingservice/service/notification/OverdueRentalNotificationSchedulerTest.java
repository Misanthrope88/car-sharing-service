package mate.academy.carsharingservice.service.notification;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import mate.academy.carsharingservice.model.Rental;
import mate.academy.carsharingservice.repository.RentalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverdueRentalNotificationSchedulerTest {
    private static final String NO_OVERDUE_MESSAGE = "No rentals overdue today!";
    private static final String FIRST_MESSAGE = "Overdue rental 1";
    private static final String SECOND_MESSAGE = "Overdue rental 2";

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationMessageBuilder messageBuilder;
    @InjectMocks
    private OverdueRentalNotificationScheduler scheduler;

    @Test
    @DisplayName("No overdue rentals sends exactly one no-overdue message")
    void checkOverdueRentals_NoRentals_SendsNoOverdueMessage() {
        // Given
        LocalDate threshold = LocalDate.now().plusDays(1);
        when(rentalRepository.findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(threshold))
                .thenReturn(List.of());
        when(messageBuilder.buildNoOverdueRentalsMessage()).thenReturn(NO_OVERDUE_MESSAGE);

        // When
        scheduler.checkOverdueRentals();

        // Then
        verify(notificationService, times(1)).sendMessage(NO_OVERDUE_MESSAGE);
    }

    @Test
    @DisplayName("Two overdue rentals send two messages")
    void checkOverdueRentals_TwoRentals_SendsMessagePerRental() {
        // Given
        LocalDate threshold = LocalDate.now().plusDays(1);
        Rental firstRental = createRental(1L);
        Rental secondRental = createRental(2L);
        when(rentalRepository.findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(threshold))
                .thenReturn(List.of(firstRental, secondRental));
        when(messageBuilder.buildOverdueRentalMessage(firstRental)).thenReturn(FIRST_MESSAGE);
        when(messageBuilder.buildOverdueRentalMessage(secondRental)).thenReturn(SECOND_MESSAGE);

        // When
        scheduler.checkOverdueRentals();

        // Then
        verify(notificationService, times(1)).sendMessage(FIRST_MESSAGE);
        verify(notificationService, times(1)).sendMessage(SECOND_MESSAGE);
        verify(notificationService, times(2)).sendMessage(anyString());
    }

    @Test
    @DisplayName("Repository is queried with tomorrow as the threshold date")
    void checkOverdueRentals_Always_QueriesWithTomorrowThreshold() {
        // Given
        LocalDate threshold = LocalDate.now().plusDays(1);
        when(rentalRepository.findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(threshold))
                .thenReturn(List.of());
        when(messageBuilder.buildNoOverdueRentalsMessage()).thenReturn(NO_OVERDUE_MESSAGE);

        // When
        scheduler.checkOverdueRentals();

        // Then
        verify(rentalRepository)
                .findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(threshold);
    }

    private Rental createRental(Long id) {
        Rental rental = new Rental();
        rental.setId(id);
        return rental;
    }
}
