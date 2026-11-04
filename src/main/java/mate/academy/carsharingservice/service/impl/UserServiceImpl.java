package mate.academy.carsharingservice.service.impl;

import java.sql.SQLException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
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
import mate.academy.carsharingservice.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final String DUPLICATE_EMAIL_MESSAGE = "Email is already registered";
    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponseDto register(UserRegistrationRequestDto request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new RegistrationException(DUPLICATE_EMAIL_MESSAGE);
        }
        User user = userMapper.toEntity(request);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);
        try {
            return userMapper.toDto(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateEmail(exception)) {
                throw new RegistrationException(DUPLICATE_EMAIL_MESSAGE);
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getByEmail(String email) {
        return userMapper.toDto(findByEmail(email));
    }

    @Override
    @Transactional
    public UserResponseDto updateByEmail(String email, UpdateUserRequestDto request) {
        User user = findByEmail(email);
        userMapper.updateUserFromDto(request, user);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponseDto updateRole(Long id, UpdateUserRoleRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Can't find user by id: " + id));
        user.setRole(request.role());
        return userMapper.toDto(userRepository.save(user));
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find user by email: " + email));
    }

    private boolean isDuplicateEmail(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException
                    && UNIQUE_VIOLATION_SQL_STATE.equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
