package mate.academy.carsharingservice.exception.handler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import mate.academy.carsharingservice.exception.CarUnavailableException;
import mate.academy.carsharingservice.exception.EntityNotFoundException;
import mate.academy.carsharingservice.exception.PaymentException;
import mate.academy.carsharingservice.exception.PaymentGatewayException;
import mate.academy.carsharingservice.exception.RegistrationException;
import mate.academy.carsharingservice.exception.RentalAlreadyReturnedException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomGlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationException(
            MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("errors", errors);
        return body;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleEntityNotFoundException(
            EntityNotFoundException exception) {
        return errorBody(exception.getMessage());
    }

    @ExceptionHandler(RegistrationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleRegistrationException(RegistrationException exception) {
        return errorBody(exception.getMessage());
    }

    @ExceptionHandler(CarUnavailableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleCarUnavailableException(CarUnavailableException exception) {
        return errorBody(exception.getMessage());
    }

    @ExceptionHandler(RentalAlreadyReturnedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleRentalAlreadyReturnedException(
            RentalAlreadyReturnedException exception) {
        return errorBody(exception.getMessage());
    }

    @ExceptionHandler(PaymentException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handlePaymentException(PaymentException exception) {
        return errorBody(exception.getMessage());
    }

    @ExceptionHandler(PaymentGatewayException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, Object> handlePaymentGatewayException() {
        return errorBody("Payment provider error");
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleBadCredentialsException() {
        return errorBody("Invalid email or password");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleAuthenticationException() {
        return errorBody("Authentication required");
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("error", message);
        return body;
    }
}
