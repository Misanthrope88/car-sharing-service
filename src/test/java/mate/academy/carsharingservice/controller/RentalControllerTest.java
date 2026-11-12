package mate.academy.carsharingservice.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import mate.academy.carsharingservice.config.AbstractIntegrationTest;
import mate.academy.carsharingservice.service.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;

@Sql(scripts = "classpath:database/controller/add-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:database/controller/clear-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RentalControllerTest extends AbstractIntegrationTest {
    private static final String MANAGER_EMAIL = "manager@carsharing.com";
    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String RENTALS_URL = "/rentals";
    private static final String RENTAL_URL = "/rentals/{id}";
    private static final String RETURN_URL = "/rentals/{id}/return";
    private static final String CAR_URL = "/cars/{id}";
    private static final int ANOTHER_CUSTOMER_ID = 102;
    private static final int AVAILABLE_CAR_ID = 101;
    private static final int UNAVAILABLE_CAR_ID = 102;
    private static final int DELETED_CAR_ID = 103;
    private static final int ACTIVE_RENTAL_ID = 101;
    private static final int RETURNED_RENTAL_ID = 102;
    private static final int ACTIVE_RENTAL_WITHOUT_PAYMENTS_ID = 103;
    private static final int FOREIGN_RENTAL_ID = 104;
    private static final int NON_EXISTING_RENTAL_ID = 999;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer rents an available car: inventory decreases and notification is sent")
    void create_AvailableCar_ReturnsCreatedRental() throws Exception {
        // Given
        String body = createRentalBody(AVAILABLE_CAR_ID, LocalDate.now().plusDays(3));

        // When
        ResultActions result = mockMvc.perform(post(RENTALS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.rentalDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.car.id").value(AVAILABLE_CAR_ID));
        mockMvc.perform(get(CAR_URL, AVAILABLE_CAR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventory").value(1));
        verify(notificationService, times(1)).sendMessage(anyString());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Renting a car with zero inventory returns 409")
    void create_ZeroInventory_ReturnsConflict() throws Exception {
        // Given
        String body = createRentalBody(UNAVAILABLE_CAR_ID, LocalDate.now().plusDays(3));

        // When
        ResultActions result = mockMvc.perform(post(RENTALS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "Car with id " + UNAVAILABLE_CAR_ID + " is not available for rental"));
        verify(notificationService, never()).sendMessage(anyString());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Renting a soft-deleted car returns 404")
    void create_DeletedCar_ReturnsNotFound() throws Exception {
        // Given
        String body = createRentalBody(DELETED_CAR_ID, LocalDate.now().plusDays(3));

        // When
        ResultActions result = mockMvc.perform(post(RENTALS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Renting with a return date in the past returns 400")
    void create_ReturnDateInPast_ReturnsBadRequest() throws Exception {
        // Given
        String body = createRentalBody(AVAILABLE_CAR_ID, LocalDate.now().minusDays(1));

        // When
        ResultActions result = mockMvc.perform(post(RENTALS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.returnDate").exists());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer sees only own rentals")
    void getAll_Customer_ReturnsOwnRentals() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(RENTALS_URL));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(ACTIVE_RENTAL_ID,
                        RETURNED_RENTAL_ID, ACTIVE_RENTAL_WITHOUT_PAYMENTS_ID)));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer filters own rentals by activity")
    void getAll_CustomerActiveFilter_ReturnsActiveRentals() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(RENTALS_URL).param("is_active", "true"));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(ACTIVE_RENTAL_ID,
                        ACTIVE_RENTAL_WITHOUT_PAYMENTS_ID)));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer filtering rentals by user id is forbidden")
    void getAll_CustomerWithUserId_ReturnsForbidden() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(RENTALS_URL)
                .param("user_id", String.valueOf(ANOTHER_CUSTOMER_ID)));

        // Then
        result.andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = MANAGER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Manager filters rentals by user id")
    void getAll_ManagerWithUserId_ReturnsRentalsOfUser() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(RENTALS_URL)
                .param("user_id", String.valueOf(ANOTHER_CUSTOMER_ID)));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(FOREIGN_RENTAL_ID)));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer getting a rental of another user is forbidden")
    void getById_CustomerOnForeignRental_ReturnsForbidden() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(RENTAL_URL, FOREIGN_RENTAL_ID));

        // Then
        result.andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = MANAGER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Manager gets a rental of any user")
    void getById_ManagerOnForeignRental_ReturnsRental() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(RENTAL_URL, FOREIGN_RENTAL_ID));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(FOREIGN_RENTAL_ID))
                .andExpect(jsonPath("$.userId").value(ANOTHER_CUSTOMER_ID));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Getting a non-existing rental returns 404")
    void getById_NonExistingId_ReturnsNotFound() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(RENTAL_URL, NON_EXISTING_RENTAL_ID));

        // Then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer returns an active rental; a second return returns 409")
    void returnRental_ActiveRental_SetsActualReturnDate() throws Exception {
        // When
        ResultActions result = mockMvc.perform(post(RETURN_URL, ACTIVE_RENTAL_ID));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.actualReturnDate").value(LocalDate.now().toString()));
        mockMvc.perform(post(RETURN_URL, ACTIVE_RENTAL_ID))
                .andExpect(status().isConflict());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Customer returning a rental of another user is forbidden")
    void returnRental_CustomerOnForeignRental_ReturnsForbidden() throws Exception {
        // When
        ResultActions result = mockMvc.perform(post(RETURN_URL, FOREIGN_RENTAL_ID));

        // Then
        result.andExpect(status().isForbidden());
    }

    private String createRentalBody(int carId, LocalDate returnDate) {
        return """
                {
                  "carId": %d,
                  "returnDate": "%s"
                }
                """.formatted(carId, returnDate);
    }
}
