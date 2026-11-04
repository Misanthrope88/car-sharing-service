package mate.academy.carsharingservice.dto;

import mate.academy.carsharingservice.model.Role;

public record UserResponseDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        Role role
) {
}
