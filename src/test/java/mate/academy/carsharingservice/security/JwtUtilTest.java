package mate.academy.carsharingservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtUtilTest {
    private static final String SECRET = "car-sharing-unit-test-secret-key-0123456789";
    private static final String OTHER_SECRET = "another-unit-test-secret-key-9876543210-abc";
    private static final String SHORT_SECRET = "too-short-secret";
    private static final long EXPIRATION_MS = 3_600_000L;
    private static final String RAW_EMAIL = "  John.Doe@Example.COM ";
    private static final String EMAIL = "john.doe@example.com";

    @Test
    @DisplayName("Generated token contains the lower-cased email as subject")
    void extractUsername_GeneratedToken_ReturnsLowerCasedEmail() {
        // Given
        JwtUtil jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
        String token = jwtUtil.generateToken(RAW_EMAIL);

        // When
        String actual = jwtUtil.extractUsername(token);

        // Then
        assertEquals(EMAIL, actual);
    }

    @Test
    @DisplayName("Token signed with another key is rejected")
    void extractUsername_TokenSignedWithOtherKey_ThrowsJwtException() {
        // Given
        JwtUtil jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
        String foreignToken = new JwtUtil(OTHER_SECRET, EXPIRATION_MS).generateToken(EMAIL);

        // When / Then
        assertThrows(JwtException.class, () -> jwtUtil.extractUsername(foreignToken));
    }

    @Test
    @DisplayName("Secret shorter than 32 bytes is rejected")
    void constructor_ShortSecret_ThrowsIllegalArgumentException() {
        // When
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new JwtUtil(SHORT_SECRET, EXPIRATION_MS));

        // Then
        assertEquals("jwt.secret must contain at least 32 UTF-8 bytes", exception.getMessage());
    }

    @Test
    @DisplayName("Non-positive expiration is rejected")
    void constructor_NonPositiveExpiration_ThrowsIllegalArgumentException() {
        // When
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new JwtUtil(SECRET, 0));

        // Then
        assertEquals("jwt.expiration-ms must be positive", exception.getMessage());
    }
}
