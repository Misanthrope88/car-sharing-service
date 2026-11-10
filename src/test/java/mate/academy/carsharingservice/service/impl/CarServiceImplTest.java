package mate.academy.carsharingservice.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import mate.academy.carsharingservice.dto.CarDto;
import mate.academy.carsharingservice.dto.CreateCarRequestDto;
import mate.academy.carsharingservice.exception.EntityNotFoundException;
import mate.academy.carsharingservice.mapper.CarMapper;
import mate.academy.carsharingservice.model.Car;
import mate.academy.carsharingservice.model.CarType;
import mate.academy.carsharingservice.repository.CarRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CarServiceImplTest {
    private static final Long CAR_ID = 1L;
    private static final Long NON_EXISTING_ID = 100L;
    private static final String MODEL = "Model S";
    private static final String BRAND = "Tesla";
    private static final int INVENTORY = 5;
    private static final BigDecimal DAILY_FEE = new BigDecimal("50.00");

    @Mock
    private CarRepository carRepository;
    @Mock
    private CarMapper carMapper;
    @InjectMocks
    private CarServiceImpl carService;

    @Test
    @DisplayName("Create a car from a valid request")
    void create_ValidRequest_ReturnsCarDto() {
        // Given
        CreateCarRequestDto request = createCarRequestDto();
        Car car = createCar(INVENTORY);
        CarDto expected = createCarDto();
        when(carMapper.toEntity(request)).thenReturn(car);
        when(carRepository.save(car)).thenReturn(car);
        when(carMapper.toDto(car)).thenReturn(expected);

        // When
        CarDto actual = carService.create(request);

        // Then
        assertEquals(expected, actual);
        verify(carRepository).save(car);
    }

    @Test
    @DisplayName("Get all active cars as a page of DTOs")
    void getAll_ActiveCarsExist_ReturnsMappedPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Car car = createCar(INVENTORY);
        CarDto dto = createCarDto();
        when(carRepository.findAllActive(pageable))
                .thenReturn(new PageImpl<>(List.of(car), pageable, 1));
        when(carMapper.toDto(car)).thenReturn(dto);

        // When
        Page<CarDto> actual = carService.getAll(pageable);

        // Then
        assertEquals(List.of(dto), actual.getContent());
        assertEquals(1, actual.getTotalElements());
    }

    @Test
    @DisplayName("Get a car by an existing id")
    void getById_ExistingId_ReturnsCarDto() {
        // Given
        Car car = createCar(INVENTORY);
        CarDto expected = createCarDto();
        when(carRepository.findActiveById(CAR_ID)).thenReturn(Optional.of(car));
        when(carMapper.toDto(car)).thenReturn(expected);

        // When
        CarDto actual = carService.getById(CAR_ID);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Get a car by a non-existing id throws an exception")
    void getById_NonExistingId_ThrowsEntityNotFoundException() {
        // Given
        when(carRepository.findActiveById(NON_EXISTING_ID)).thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> carService.getById(NON_EXISTING_ID));

        // Then
        assertEquals("Can't find car by id: " + NON_EXISTING_ID, exception.getMessage());
    }

    @Test
    @DisplayName("Update an existing car")
    void update_ExistingId_ReturnsUpdatedCarDto() {
        // Given
        CreateCarRequestDto request = createCarRequestDto();
        Car car = createCar(INVENTORY);
        CarDto expected = createCarDto();
        when(carRepository.findActiveById(CAR_ID)).thenReturn(Optional.of(car));
        when(carRepository.save(car)).thenReturn(car);
        when(carMapper.toDto(car)).thenReturn(expected);

        // When
        CarDto actual = carService.update(CAR_ID, request);

        // Then
        assertEquals(expected, actual);
        verify(carMapper).updateCarFromDto(request, car);
        verify(carRepository).save(car);
    }

    @Test
    @DisplayName("Update a non-existing car throws an exception")
    void update_NonExistingId_ThrowsEntityNotFoundException() {
        // Given
        CreateCarRequestDto request = createCarRequestDto();
        when(carRepository.findActiveById(NON_EXISTING_ID)).thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> carService.update(NON_EXISTING_ID, request));

        // Then
        assertEquals("Can't find car by id: " + NON_EXISTING_ID, exception.getMessage());
        verify(carRepository, never()).save(any(Car.class));
    }

    @Test
    @DisplayName("Delete an existing car")
    void deleteById_ExistingId_DeletesCar() {
        // Given
        Car car = createCar(INVENTORY);
        when(carRepository.findActiveById(CAR_ID)).thenReturn(Optional.of(car));

        // When
        carService.deleteById(CAR_ID);

        // Then
        verify(carRepository).delete(car);
    }

    @Test
    @DisplayName("Delete a non-existing car throws an exception")
    void deleteById_NonExistingId_ThrowsEntityNotFoundException() {
        // Given
        when(carRepository.findActiveById(NON_EXISTING_ID)).thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> carService.deleteById(NON_EXISTING_ID));

        // Then
        assertEquals("Can't find car by id: " + NON_EXISTING_ID, exception.getMessage());
        verify(carRepository, never()).delete(any(Car.class));
    }

    private CreateCarRequestDto createCarRequestDto() {
        return new CreateCarRequestDto(MODEL, BRAND, CarType.SEDAN, INVENTORY, DAILY_FEE);
    }

    private Car createCar(int inventory) {
        Car car = new Car();
        car.setId(CAR_ID);
        car.setModel(MODEL);
        car.setBrand(BRAND);
        car.setType(CarType.SEDAN);
        car.setInventory(inventory);
        car.setDailyFee(DAILY_FEE);
        return car;
    }

    private CarDto createCarDto() {
        return new CarDto(CAR_ID, MODEL, BRAND, CarType.SEDAN, INVENTORY, DAILY_FEE);
    }
}
