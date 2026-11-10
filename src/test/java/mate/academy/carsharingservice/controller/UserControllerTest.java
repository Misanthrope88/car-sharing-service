package mate.academy.carsharingservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mate.academy.carsharingservice.config.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;

@Sql(scripts = "classpath:database/controller/add-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:database/controller/clear-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class UserControllerTest extends AbstractIntegrationTest {
    private static final String MANAGER_EMAIL = "manager@carsharing.com";
    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final int CUSTOMER_ID = 101;
    private static final String ME_URL = "/users/me";
    private static final String ROLE_URL = "/users/{id}/role";
    private static final String MANAGER_ROLE_BODY = """
            {
              "role": "MANAGER"
            }
            """;

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer gets own profile")
    void getMe_Customer_ReturnsOwnProfile() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(ME_URL));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_ID))
                .andExpect(jsonPath("$.email").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer updates own first and last name")
    void updateMe_Customer_ReturnsUpdatedProfile() throws Exception {
        // Given
        String body = """
                {
                  "firstName": "Johnny",
                  "lastName": "Walker"
                }
                """;

        // When
        ResultActions result = mockMvc.perform(put(ME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.lastName").value("Walker"));
    }

    @Test
    @WithUserDetails(value = MANAGER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Manager changes the role of a user")
    void updateRole_Manager_ReturnsUserWithNewRole() throws Exception {
        // When
        ResultActions result = mockMvc.perform(put(ROLE_URL, CUSTOMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(MANAGER_ROLE_BODY));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CUSTOMER_ID))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer changing a role is forbidden")
    void updateRole_Customer_ReturnsForbidden() throws Exception {
        // When
        ResultActions result = mockMvc.perform(put(ROLE_URL, CUSTOMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(MANAGER_ROLE_BODY));

        // Then
        result.andExpect(status().isForbidden());
    }
}
