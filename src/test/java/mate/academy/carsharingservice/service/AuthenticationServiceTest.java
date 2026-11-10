package mate.academy.carsharingservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import mate.academy.carsharingservice.dto.UserLoginRequestDto;
import mate.academy.carsharingservice.dto.UserLoginResponseDto;
import mate.academy.carsharingservice.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    private static final String EMAIL = "john.doe@example.com";
    private static final String PASSWORD = "password123";
    private static final String TOKEN = "jwt-token";
    private static final String TOO_LONG_PASSWORD = "я".repeat(37);

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("Login with valid credentials returns a JWT token")
    void login_ValidCredentials_ReturnsToken() {
        // Given
        UserLoginRequestDto request = new UserLoginRequestDto(EMAIL, PASSWORD);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn(EMAIL);
        when(jwtUtil.generateToken(EMAIL)).thenReturn(TOKEN);

        // When
        UserLoginResponseDto actual = authenticationService.login(request);

        // Then
        assertEquals(TOKEN, actual.token());
    }

    @Test
    @DisplayName("Login with a password longer than 72 UTF-8 bytes is rejected early")
    void login_PasswordLongerThan72Bytes_ThrowsBadCredentialsException() {
        // Given
        UserLoginRequestDto request = new UserLoginRequestDto(EMAIL, TOO_LONG_PASSWORD);

        // When
        assertThrows(BadCredentialsException.class, () -> authenticationService.login(request));

        // Then
        verify(authenticationManager, never()).authenticate(any(Authentication.class));
    }

    @Test
    @DisplayName("Login with wrong credentials propagates the authentication exception")
    void login_InvalidCredentials_PropagatesBadCredentialsException() {
        // Given
        UserLoginRequestDto request = new UserLoginRequestDto(EMAIL, PASSWORD);
        BadCredentialsException expected = new BadCredentialsException("Bad credentials");
        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(expected);

        // When
        BadCredentialsException actual = assertThrows(BadCredentialsException.class,
                () -> authenticationService.login(request));

        // Then
        assertSame(expected, actual);
        verify(jwtUtil, never()).generateToken(any());
    }
}
