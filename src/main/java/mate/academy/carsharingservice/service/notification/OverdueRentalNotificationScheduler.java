package mate.academy.carsharingservice.service.notification;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mate.academy.carsharingservice.model.Rental;
import mate.academy.carsharingservice.repository.RentalRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OverdueRentalNotificationScheduler {
    private final RentalRepository rentalRepository;
    private final NotificationService notificationService;
    private final NotificationMessageBuilder messageBuilder;

    @Scheduled(cron = "${rental.overdue-check.cron}", zone = "${rental.overdue-check.zone}")
    public void checkOverdueRentals() {
        LocalDate threshold = LocalDate.now().plusDays(1);
        List<Rental> rentals = rentalRepository
                .findAllByActualReturnDateIsNullAndReturnDateLessThanEqual(threshold);
        if (rentals.isEmpty()) {
            notificationService.sendMessage(messageBuilder.buildNoOverdueRentalsMessage());
            return;
        }
        for (Rental rental : rentals) {
            notificationService.sendMessage(messageBuilder.buildOverdueRentalMessage(rental));
        }
    }
}
