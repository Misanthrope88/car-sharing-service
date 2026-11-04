package mate.academy.carsharingservice.dto;

import jakarta.validation.constraints.NotNull;
import mate.academy.carsharingservice.model.Role;

public record UpdateUserRoleRequestDto(
        @NotNull(message = "Role must not be null")
        Role role
) {
}
