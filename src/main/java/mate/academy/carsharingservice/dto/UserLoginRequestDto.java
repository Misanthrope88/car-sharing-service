package mate.academy.carsharingservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;

public record UserLoginRequestDto(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be valid")
        String email,
        @NotBlank(message = "Password must not be blank")
        String password
) {
    public UserLoginRequestDto {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
