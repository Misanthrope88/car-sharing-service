package mate.academy.carsharingservice.exception.handler;

import java.util.LinkedHashMap;
import java.util.Map;
import mate.academy.carsharingservice.exception.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomGlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationException(
            MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return errors;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleEntityNotFoundException(
            EntityNotFoundException exception) {
        return Map.of("error", exception.getMessage());
    }
}
