package mate.academy.carsharingservice.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.validation.ConstraintValidatorContext;
import mate.academy.carsharingservice.dto.UserRegistrationRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FieldMatchValidatorTest {
    private static final String EMAIL = "john.doe@example.com";
    private static final String PASSWORD = "password123";
    private static final String OTHER_PASSWORD = "different123";

    private final FieldMatchValidator validator = new FieldMatchValidator();

    @Test
    @DisplayName("Matching passwords are valid")
    void isValid_EqualPasswords_ReturnsTrue() {
        // Given
        ConstraintValidatorContext context =
                mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);
        UserRegistrationRequestDto request = createRequest(PASSWORD, PASSWORD);

        // When
        boolean actual = validator.isValid(request, context);

        // Then
        assertTrue(actual);
        verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    @DisplayName("Different passwords are invalid and reported on repeatPassword")
    void isValid_DifferentPasswords_ReturnsFalse() {
        // Given
        ConstraintValidatorContext context =
                mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);
        UserRegistrationRequestDto request = createRequest(PASSWORD, OTHER_PASSWORD);

        // When
        boolean actual = validator.isValid(request, context);

        // Then
        assertFalse(actual);
        verify(context).disableDefaultConstraintViolation();
    }

    private UserRegistrationRequestDto createRequest(String password, String repeatPassword) {
        return new UserRegistrationRequestDto(EMAIL, password, repeatPassword, "John", "Doe");
    }
}
