package mate.academy.carsharingservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import mate.academy.carsharingservice.model.Role;
import mate.academy.carsharingservice.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:database/repository/add-repository-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:database/repository/clear-repository-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class UserRepositoryTest extends AbstractRepositoryTest {
    private static final Long CUSTOMER_ID = 101L;
    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String UNKNOWN_EMAIL = "unknown@example.com";
    private static final String MANAGER_EMAIL = "manager@carsharing.com";

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Find a user by an existing email")
    void findByEmail_ExistingEmail_ReturnsUser() {
        // When
        Optional<User> actual = userRepository.findByEmail(CUSTOMER_EMAIL);

        // Then
        assertTrue(actual.isPresent());
        assertEquals(CUSTOMER_ID, actual.get().getId());
        assertEquals(Role.CUSTOMER, actual.get().getRole());
    }

    @Test
    @DisplayName("Find a user by a non-existing email returns empty")
    void findByEmail_NonExistingEmail_ReturnsEmpty() {
        // When
        Optional<User> actual = userRepository.findByEmail(UNKNOWN_EMAIL);

        // Then
        assertTrue(actual.isEmpty());
    }

    @Test
    @DisplayName("Existing email is reported as taken")
    void existsByEmail_ExistingEmail_ReturnsTrue() {
        // When
        boolean actual = userRepository.existsByEmail(CUSTOMER_EMAIL);

        // Then
        assertTrue(actual);
    }

    @Test
    @DisplayName("Unknown email is reported as free")
    void existsByEmail_NonExistingEmail_ReturnsFalse() {
        // When
        boolean actual = userRepository.existsByEmail(UNKNOWN_EMAIL);

        // Then
        assertFalse(actual);
    }

    @Test
    @DisplayName("Liquibase seed manager exists with MANAGER role")
    void findByEmail_SeedManager_ReturnsManager() {
        // When
        Optional<User> actual = userRepository.findByEmail(MANAGER_EMAIL);

        // Then
        assertTrue(actual.isPresent());
        assertEquals(Role.MANAGER, actual.get().getRole());
    }
}
