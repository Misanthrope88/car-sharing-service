package mate.academy.carsharingservice.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import mate.academy.carsharingservice.dto.UpdateUserRequestDto;
import mate.academy.carsharingservice.dto.UpdateUserRoleRequestDto;
import mate.academy.carsharingservice.dto.UserRegistrationRequestDto;
import mate.academy.carsharingservice.dto.UserResponseDto;
import mate.academy.carsharingservice.exception.EntityNotFoundException;
import mate.academy.carsharingservice.exception.RegistrationException;
import mate.academy.carsharingservice.mapper.UserMapper;
import mate.academy.carsharingservice.model.Role;
import mate.academy.carsharingservice.model.User;
import mate.academy.carsharingservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    private static final Long USER_ID = 1L;
    private static final Long NON_EXISTING_ID = 100L;
    private static final String RAW_EMAIL = "  John.Doe@Example.COM  ";
    private static final String EMAIL = "john.doe@example.com";
    private static final String PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Register a new user with encoded password, CUSTOMER role and normalized email")
    void register_ValidRequest_SavesCustomerWithEncodedPassword() {
        // Given
        UserRegistrationRequestDto request = createRegistrationRequest();
        User mappedUser = createUser(null, null);
        UserResponseDto expected = createUserResponseDto(Role.CUSTOMER);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(mappedUser)).thenReturn(mappedUser);
        when(userMapper.toDto(mappedUser)).thenReturn(expected);

        // When
        UserResponseDto actual = userService.register(request);

        // Then
        assertEquals(expected, actual);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(EMAIL, savedUser.getEmail());
        assertEquals(ENCODED_PASSWORD, savedUser.getPassword());
        assertEquals(Role.CUSTOMER, savedUser.getRole());
    }

    @Test
    @DisplayName("Register with an already used email throws an exception and saves nothing")
    void register_EmailAlreadyExists_ThrowsRegistrationException() {
        // Given
        UserRegistrationRequestDto request = createRegistrationRequest();
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        // When
        RegistrationException exception = assertThrows(RegistrationException.class,
                () -> userService.register(request));

        // Then
        assertEquals("Email is already registered: " + EMAIL, exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Get a user by an existing email")
    void getByEmail_ExistingEmail_ReturnsUserDto() {
        // Given
        User user = createUser(USER_ID, Role.CUSTOMER);
        UserResponseDto expected = createUserResponseDto(Role.CUSTOMER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expected);

        // When
        UserResponseDto actual = userService.getByEmail(EMAIL);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Get a user by a non-existing email throws an exception")
    void getByEmail_NonExistingEmail_ThrowsEntityNotFoundException() {
        // Given
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.getByEmail(EMAIL));

        // Then
        assertEquals("Can't find user by email: " + EMAIL, exception.getMessage());
    }

    @Test
    @DisplayName("Update the profile of an existing user")
    void updateByEmail_ExistingEmail_ReturnsUpdatedUserDto() {
        // Given
        User user = createUser(USER_ID, Role.CUSTOMER);
        UpdateUserRequestDto request = new UpdateUserRequestDto("Jane", "Smith");
        UserResponseDto expected = createUserResponseDto(Role.CUSTOMER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expected);

        // When
        UserResponseDto actual = userService.updateByEmail(EMAIL, request);

        // Then
        assertEquals(expected, actual);
        verify(userMapper).updateUserFromDto(request, user);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Update the role of an existing user")
    void updateRole_ExistingId_SetsNewRole() {
        // Given
        User user = createUser(USER_ID, Role.CUSTOMER);
        UpdateUserRoleRequestDto request = new UpdateUserRoleRequestDto(Role.MANAGER);
        UserResponseDto expected = createUserResponseDto(Role.MANAGER);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(expected);

        // When
        UserResponseDto actual = userService.updateRole(USER_ID, request);

        // Then
        assertEquals(expected, actual);
        assertEquals(Role.MANAGER, user.getRole());
    }

    @Test
    @DisplayName("Update the role of a non-existing user throws an exception")
    void updateRole_NonExistingId_ThrowsEntityNotFoundException() {
        // Given
        UpdateUserRoleRequestDto request = new UpdateUserRoleRequestDto(Role.MANAGER);
        when(userRepository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.updateRole(NON_EXISTING_ID, request));

        // Then
        assertEquals("Can't find user by id: " + NON_EXISTING_ID, exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    private UserRegistrationRequestDto createRegistrationRequest() {
        return new UserRegistrationRequestDto(
                RAW_EMAIL, PASSWORD, PASSWORD, FIRST_NAME, LAST_NAME);
    }

    private User createUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setRole(role);
        if (id != null) {
            user.setEmail(EMAIL);
            user.setPassword(ENCODED_PASSWORD);
        }
        return user;
    }

    private UserResponseDto createUserResponseDto(Role role) {
        return new UserResponseDto(USER_ID, EMAIL, FIRST_NAME, LAST_NAME, role);
    }
}
