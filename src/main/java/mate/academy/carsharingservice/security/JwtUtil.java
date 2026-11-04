package mate.academy.carsharingservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final JwtParser parser;
    private final long expirationMillis;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long expirationMillis) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("jwt.secret must be configured using JWT_SECRET");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("jwt.secret must contain at least 32 UTF-8 bytes");
        }
        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("jwt.expiration-ms must be positive");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.parser = Jwts.parser()
                .verifyWith(key)
                .sig().clear().add(Jwts.SIG.HS256).and()
                .build();
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email.trim().toLowerCase(Locale.ROOT))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        if (!StringUtils.hasText(claims.getSubject()) || claims.getExpiration() == null) {
            throw new MalformedJwtException("JWT must contain a subject and an expiration");
        }
        return claims.getSubject().trim().toLowerCase(Locale.ROOT);
    }
}
