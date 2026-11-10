package mate.academy.carsharingservice.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class CarControllerTest extends AbstractIntegrationTest {
    private static final String MANAGER_EMAIL = "manager@carsharing.com";
    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String CARS_URL = "/cars";
    private static final String CAR_URL = "/cars/{id}";
    private static final int ACTIVE_CAR_ID = 101;
    private static final int UNAVAILABLE_CAR_ID = 102;
    private static final int DELETED_CAR_ID = 103;
    private static final String VALID_CAR_BODY = """
            {
              "model": "Civic",
              "brand": "Honda",
              "type": "SEDAN",
              "inventory": 4,
              "dailyFee": 45.00
            }
            """;

    @Test
    @DisplayName("Anonymous user gets active cars without the soft-deleted one")
    void getAll_Anonymous_ReturnsActiveCars() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(CARS_URL));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id",
                        hasItems(ACTIVE_CAR_ID, UNAVAILABLE_CAR_ID)))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(DELETED_CAR_ID))));
    }

    @Test
    @DisplayName("Anonymous user gets an active car by id")
    void getById_ActiveCar_ReturnsCar() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(CAR_URL, ACTIVE_CAR_ID));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACTIVE_CAR_ID))
                .andExpect(jsonPath("$.brand").value("Toyota"));
    }

    @Test
    @DisplayName("Getting a soft-deleted car returns 404")
    void getById_SoftDeletedCar_ReturnsNotFound() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(CAR_URL, DELETED_CAR_ID));

        // Then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithUserDetails(value = MANAGER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Manager creates a car")
    void create_Manager_ReturnsCreated() throws Exception {
        // When
        ResultActions result = mockMvc.perform(post(CARS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CAR_BODY));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.brand").value("Honda"))
                .andExpect(jsonPath("$.inventory").value(4));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer creating a car is forbidden")
    void create_Customer_ReturnsForbidden() throws Exception {
        // When
        ResultActions result = mockMvc.perform(post(CARS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CAR_BODY));

        // Then
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    @DisplayName("Anonymous user creating a car is unauthorized")
    void create_Anonymous_ReturnsUnauthorized() throws Exception {
        // When
        ResultActions result = mockMvc.perform(post(CARS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CAR_BODY));

        // Then
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithUserDetails(value = MANAGER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Manager creating a car with invalid data gets 400")
    void create_InvalidBody_ReturnsBadRequest() throws Exception {
        // Given
        String body = """
                {
                  "model": "",
                  "brand": "Honda",
                  "type": "SEDAN",
                  "inventory": -1,
                  "dailyFee": 0
                }
                """;

        // When
        ResultActions result = mockMvc.perform(post(CARS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors.model").exists())
                .andExpect(jsonPath("$.errors.inventory").exists())
                .andExpect(jsonPath("$.errors.dailyFee").exists());
    }

    @Test
    @WithUserDetails(value = MANAGER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Manager updates a car")
    void update_Manager_ReturnsUpdatedCar() throws Exception {
        // Given
        String body = """
                {
                  "model": "Corolla",
                  "brand": "Toyota",
                  "type": "SEDAN",
                  "inventory": 7,
                  "dailyFee": 40.00
                }
                """;

        // When
        ResultActions result = mockMvc.perform(put(CAR_URL, ACTIVE_CAR_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACTIVE_CAR_ID))
                .andExpect(jsonPath("$.inventory").value(7));
    }

    @Test
    @WithUserDetails(value = MANAGER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Manager deletes a car and it is no longer available")
    void deleteById_Manager_SoftDeletesCar() throws Exception {
        // When
        ResultActions result = mockMvc.perform(delete(CAR_URL, ACTIVE_CAR_ID));

        // Then
        result.andExpect(status().isNoContent());
        mockMvc.perform(get(CAR_URL, ACTIVE_CAR_ID))
                .andExpect(status().isNotFound());
    }
}
