package mate.academy.carsharingservice.exception;

public class CarUnavailableException extends RuntimeException {
    public CarUnavailableException(String message) {
        super(message);
    }
}
