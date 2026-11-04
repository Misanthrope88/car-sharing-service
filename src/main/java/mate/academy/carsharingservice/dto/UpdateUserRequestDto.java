package mate.academy.carsharingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequestDto(
        @NotBlank(message = "First name must not be blank")
        @Size(max = 255, message = "First name must not exceed 255 characters")
        String firstName,
        @NotBlank(message = "Last name must not be blank")
        @Size(max = 255, message = "Last name must not exceed 255 characters")
        String lastName
) {
}
