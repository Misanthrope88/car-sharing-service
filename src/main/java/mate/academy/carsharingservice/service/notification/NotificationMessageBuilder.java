package mate.academy.carsharingservice.service.notification;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import mate.academy.carsharingservice.model.Car;
import mate.academy.carsharingservice.model.Rental;
import mate.academy.carsharingservice.model.User;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageBuilder {
    public String buildNewRentalMessage(Rental rental) {
        User user = rental.getUser();
        Car car = rental.getCar();
        return String.join("\n",
                "New rental created",
                "Rental ID: " + rental.getId(),
                "Customer: " + user.getFirstName() + " " + user.getLastName()
                        + " (" + user.getEmail() + ")",
                "Car: " + car.getBrand() + " " + car.getModel() + " (" + car.getType() + ")",
                "Daily fee: $" + car.getDailyFee(),
                "Rental date: " + rental.getRentalDate(),
                "Return date: " + rental.getReturnDate());
    }

    public String buildOverdueRentalMessage(Rental rental) {
        User user = rental.getUser();
        Car car = rental.getCar();
        long daysOverdue = Math.max(0,
                ChronoUnit.DAYS.between(rental.getReturnDate(), LocalDate.now()));
        return String.join("\n",
                "Overdue rental",
                "Rental ID: " + rental.getId(),
                "Customer: " + user.getFirstName() + " " + user.getLastName()
                        + " (" + user.getEmail() + ")",
                "Car: " + car.getBrand() + " " + car.getModel() + " (" + car.getType() + ")",
                "Rental date: " + rental.getRentalDate(),
                "Return date: " + rental.getReturnDate(),
                "Days overdue: " + daysOverdue);
    }

    public String buildNoOverdueRentalsMessage() {
        return "No rentals overdue today!";
    }
}
