package mate.academy.carsharingservice.controller;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mate.academy.carsharingservice.config.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;

@Sql(scripts = "classpath:database/controller/add-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:database/controller/clear-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AuthenticationControllerTest extends AbstractIntegrationTest {
    private static final String REGISTER_URL = "/register";
    private static final String LOGIN_URL = "/login";
    private static final String NEW_EMAIL = "new.user@example.com";

    @Test
    @DisplayName("Register a new customer with a mixed-case email")
    void register_ValidRequest_ReturnsCreatedCustomer() throws Exception {
        // Given
        String body = """
                {
                  "email": "New.User@Example.COM",
                  "password": "password123",
                  "repeatPassword": "password123",
                  "firstName": "New",
                  "lastName": "User"
                }
                """;

        // When
        ResultActions result = mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(NEW_EMAIL))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Register with an already registered email returns 409")
    void register_ExistingEmail_ReturnsConflict() throws Exception {
        // Given
        String body = """
                {
                  "email": "customer@example.com",
                  "password": "password123",
                  "repeatPassword": "password123",
                  "firstName": "John",
                  "lastName": "Doe"
                }
                """;

        // When
        ResultActions result = mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.error").value("Email is already registered"));
    }

    @Test
    @DisplayName("Register with different passwords returns 400 for repeatPassword")
    void register_PasswordsDoNotMatch_ReturnsBadRequest() throws Exception {
        // Given
        String body = """
                {
                  "email": "mismatch@example.com",
                  "password": "password123",
                  "repeatPassword": "different123",
                  "firstName": "Mis",
                  "lastName": "Match"
                }
                """;

        // When
        ResultActions result = mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.repeatPassword").exists());
    }

    @Test
    @DisplayName("Login with valid manager credentials returns a token")
    void login_ValidCredentials_ReturnsToken() throws Exception {
        // Given
        String body = """
                {
                  "email": "manager@carsharing.com",
                  "password": "Manager12345"
                }
                """;

        // When
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(not(emptyString())));
    }

    @Test
    @DisplayName("Login with a wrong password returns 401")
    void login_WrongPassword_ReturnsUnauthorized() throws Exception {
        // Given
        String body = """
                {
                  "email": "manager@carsharing.com",
                  "password": "WrongPassword1"
                }
                """;

        // When
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }
}
