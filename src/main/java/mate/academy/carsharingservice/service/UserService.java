package mate.academy.carsharingservice.service;

import mate.academy.carsharingservice.dto.UserRegistrationRequestDto;
import mate.academy.carsharingservice.dto.UserResponseDto;

public interface UserService {
    UserResponseDto register(UserRegistrationRequestDto request);
}
