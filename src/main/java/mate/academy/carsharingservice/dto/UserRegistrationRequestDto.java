package mate.academy.carsharingservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import mate.academy.carsharingservice.validation.FieldMatch;

@FieldMatch(message = "Passwords must match")
public record UserRegistrationRequestDto(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,
        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters")
        String password,
        @NotBlank(message = "Repeated password must not be blank")
        String repeatPassword,
        @NotBlank(message = "First name must not be blank")
        @Size(max = 255, message = "First name must not exceed 255 characters")
        String firstName,
        @NotBlank(message = "Last name must not be blank")
        @Size(max = 255, message = "Last name must not exceed 255 characters")
        String lastName
) {
    public UserRegistrationRequestDto {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }

    @AssertTrue(message = "Password must not exceed 72 UTF-8 bytes")
    @JsonIgnore
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
