package mate.academy.carsharingservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import mate.academy.carsharingservice.config.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;

@Sql(scripts = "classpath:database/controller/add-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:database/controller/clear-controller-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class HealthControllerTest extends AbstractIntegrationTest {
    private static final String HEALTH_URL = "/health";

    @Test
    @DisplayName("Health endpoint is available anonymously and reports UP")
    void health_Anonymous_ReturnsUp() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get(HEALTH_URL));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
