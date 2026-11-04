package mate.academy.carsharingservice.service;

import mate.academy.carsharingservice.dto.UpdateUserRequestDto;
import mate.academy.carsharingservice.dto.UpdateUserRoleRequestDto;
import mate.academy.carsharingservice.dto.UserRegistrationRequestDto;
import mate.academy.carsharingservice.dto.UserResponseDto;

public interface UserService {
    UserResponseDto register(UserRegistrationRequestDto request);

    UserResponseDto getByEmail(String email);

    UserResponseDto updateByEmail(String email, UpdateUserRequestDto request);

    UserResponseDto updateRole(Long id, UpdateUserRoleRequestDto request);
}
