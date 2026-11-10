package mate.academy.carsharingservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import mate.academy.carsharingservice.model.Car;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "classpath:database/repository/add-repository-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:database/repository/clear-repository-data.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CarRepositoryTest extends AbstractRepositoryTest {
    private static final Long TOYOTA_ID = 101L;
    private static final Long TESLA_ID = 102L;
    private static final Long DELETED_CAR_ID = 103L;
    private static final String TOYOTA_BRAND = "Toyota";

    @Autowired
    private CarRepository carRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Find all active cars excludes the soft-deleted car")
    void findAllActive_WithSoftDeletedCar_ReturnsOnlyActiveCars() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("id"));

        // When
        Page<Car> actual = carRepository.findAllActive(pageable);

        // Then
        List<Long> testCarIds = actual.getContent().stream()
                .map(Car::getId)
                .filter(id -> id >= TOYOTA_ID)
                .toList();
        assertEquals(List.of(TOYOTA_ID, TESLA_ID), testCarIds);
    }

    @Test
    @DisplayName("Find an active car by id")
    void findActiveById_ActiveCar_ReturnsCar() {
        // When
        Optional<Car> actual = carRepository.findActiveById(TOYOTA_ID);

        // Then
        assertTrue(actual.isPresent());
        assertEquals(TOYOTA_BRAND, actual.get().getBrand());
    }

    @Test
    @DisplayName("Find a soft-deleted car as active returns empty")
    void findActiveById_SoftDeletedCar_ReturnsEmpty() {
        // When
        Optional<Car> actual = carRepository.findActiveById(DELETED_CAR_ID);

        // Then
        assertTrue(actual.isEmpty());
    }

    @Test
    @DisplayName("Lock a soft-deleted car as active returns empty")
    void findActiveByIdForUpdate_SoftDeletedCar_ReturnsEmpty() {
        // When
        Optional<Car> actual = carRepository.findActiveByIdForUpdate(DELETED_CAR_ID);

        // Then
        assertTrue(actual.isEmpty());
    }

    @Test
    @DisplayName("Lock a car by id also returns a soft-deleted car")
    void findByIdForUpdate_SoftDeletedCar_ReturnsCar() {
        // When
        Optional<Car> actual = carRepository.findByIdForUpdate(DELETED_CAR_ID);

        // Then
        assertTrue(actual.isPresent());
        assertTrue(actual.get().isDeleted());
    }

    @Test
    @DisplayName("Deleting a car marks it as deleted instead of removing the row")
    void delete_ActiveCar_PerformsSoftDelete() {
        // Given
        Car car = carRepository.findActiveById(TESLA_ID).orElseThrow();

        // When
        carRepository.delete(car);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertFalse(carRepository.findActiveById(TESLA_ID).isPresent());
        Optional<Car> deletedCar = carRepository.findById(TESLA_ID);
        assertTrue(deletedCar.isPresent());
        assertTrue(deletedCar.get().isDeleted());
    }
}
