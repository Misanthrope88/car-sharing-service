package mate.academy.carsharingservice.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import mate.academy.carsharingservice.dto.CreateRentalRequestDto;
import mate.academy.carsharingservice.dto.RentalResponseDto;
import mate.academy.carsharingservice.exception.CarUnavailableException;
import mate.academy.carsharingservice.exception.EntityNotFoundException;
import mate.academy.carsharingservice.exception.RentalAlreadyReturnedException;
import mate.academy.carsharingservice.mapper.RentalMapper;
import mate.academy.carsharingservice.model.Car;
import mate.academy.carsharingservice.model.CarType;
import mate.academy.carsharingservice.model.Rental;
import mate.academy.carsharingservice.model.Role;
import mate.academy.carsharingservice.model.User;
import mate.academy.carsharingservice.repository.CarRepository;
import mate.academy.carsharingservice.repository.RentalRepository;
import mate.academy.carsharingservice.service.notification.NotificationMessageBuilder;
import mate.academy.carsharingservice.service.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class RentalServiceImplTest {
    private static final Long CUSTOMER_ID = 1L;
    private static final Long OTHER_CUSTOMER_ID = 2L;
    private static final Long MANAGER_ID = 3L;
    private static final Long CAR_ID = 10L;
    private static final Long RENTAL_ID = 20L;
    private static final Long NON_EXISTING_ID = 100L;
    private static final int INVENTORY = 3;
    private static final BigDecimal DAILY_FEE = new BigDecimal("10.00");
    private static final String NEW_RENTAL_MESSAGE = "New rental created";

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private CarRepository carRepository;
    @Mock
    private RentalMapper rentalMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationMessageBuilder messageBuilder;
    @InjectMocks
    private RentalServiceImpl rentalService;

    @Test
    @DisplayName("Create a rental: inventory decremented, rental saved and notification sent")
    void create_AvailableCar_SavesRentalAndSendsNotification() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Car car = createCar(INVENTORY);
        LocalDate returnDate = LocalDate.now().plusDays(3);
        CreateRentalRequestDto request = new CreateRentalRequestDto(CAR_ID, returnDate);
        RentalResponseDto expected = createRentalResponseDto();
        when(carRepository.findActiveByIdForUpdate(CAR_ID)).thenReturn(Optional.of(car));
        when(rentalRepository.save(any(Rental.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(messageBuilder.buildNewRentalMessage(any(Rental.class)))
                .thenReturn(NEW_RENTAL_MESSAGE);
        when(rentalMapper.toDto(any(Rental.class))).thenReturn(expected);

        // When
        RentalResponseDto actual = rentalService.create(request, customer);

        // Then
        assertEquals(expected, actual);
        assertEquals(INVENTORY - 1, car.getInventory());
        ArgumentCaptor<Rental> rentalCaptor = ArgumentCaptor.forClass(Rental.class);
        verify(rentalRepository).save(rentalCaptor.capture());
        Rental savedRental = rentalCaptor.getValue();
        assertEquals(LocalDate.now(), savedRental.getRentalDate());
        assertEquals(returnDate, savedRental.getReturnDate());
        assertEquals(customer, savedRental.getUser());
        assertEquals(car, savedRental.getCar());
        verify(notificationService, times(1)).sendMessage(NEW_RENTAL_MESSAGE);
    }

    @Test
    @DisplayName("Create a rental for a non-existing car throws an exception")
    void create_NonExistingCar_ThrowsEntityNotFoundException() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        CreateRentalRequestDto request = new CreateRentalRequestDto(
                NON_EXISTING_ID, LocalDate.now().plusDays(3));
        when(carRepository.findActiveByIdForUpdate(NON_EXISTING_ID))
                .thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> rentalService.create(request, customer));

        // Then
        assertEquals("Can't find car by id: " + NON_EXISTING_ID, exception.getMessage());
        verify(rentalRepository, never()).save(any(Rental.class));
    }

    @Test
    @DisplayName("Create a rental for a car with zero inventory throws an exception")
    void create_ZeroInventory_ThrowsCarUnavailableException() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Car car = createCar(0);
        CreateRentalRequestDto request = new CreateRentalRequestDto(
                CAR_ID, LocalDate.now().plusDays(3));
        when(carRepository.findActiveByIdForUpdate(CAR_ID)).thenReturn(Optional.of(car));

        // When
        assertThrows(CarUnavailableException.class,
                () -> rentalService.create(request, customer));

        // Then
        assertEquals(0, car.getInventory());
        verify(rentalRepository, never()).save(any(Rental.class));
        verify(notificationService, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Customer filtering rentals by user id is denied")
    void getAll_CustomerWithUserId_ThrowsAccessDeniedException() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Pageable pageable = PageRequest.of(0, 10);

        // When
        assertThrows(AccessDeniedException.class,
                () -> rentalService.getAll(customer, OTHER_CUSTOMER_ID, null, pageable));

        // Then
        verify(rentalRepository, never()).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Customer without user filter gets a mapped page of rentals")
    void getAll_CustomerWithoutUserId_ReturnsMappedPage() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Pageable pageable = PageRequest.of(0, 10);
        RentalResponseDto dto = stubFindAllWithOneRental(pageable);

        // When
        Page<RentalResponseDto> actual = rentalService.getAll(customer, null, true, pageable);

        // Then
        assertEquals(List.of(dto), actual.getContent());
        verify(rentalRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Manager without user filter gets a mapped page of rentals")
    void getAll_ManagerWithoutUserId_ReturnsMappedPage() {
        // Given
        User manager = createUser(MANAGER_ID, Role.MANAGER);
        Pageable pageable = PageRequest.of(0, 10);
        RentalResponseDto dto = stubFindAllWithOneRental(pageable);

        // When
        Page<RentalResponseDto> actual = rentalService.getAll(manager, null, null, pageable);

        // Then
        assertEquals(List.of(dto), actual.getContent());
        verify(rentalRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Manager with user filter gets a mapped page of rentals")
    void getAll_ManagerWithUserId_ReturnsMappedPage() {
        // Given
        User manager = createUser(MANAGER_ID, Role.MANAGER);
        Pageable pageable = PageRequest.of(0, 10);
        RentalResponseDto dto = stubFindAllWithOneRental(pageable);

        // When
        Page<RentalResponseDto> actual =
                rentalService.getAll(manager, CUSTOMER_ID, false, pageable);

        // Then
        assertEquals(List.of(dto), actual.getContent());
        verify(rentalRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Customer gets own rental by id")
    void getById_OwnRental_ReturnsRentalDto() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Rental rental = createRental(customer, createCar(INVENTORY), null);
        RentalResponseDto expected = createRentalResponseDto();
        when(rentalRepository.findWithDetailsById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(rentalMapper.toDto(rental)).thenReturn(expected);

        // When
        RentalResponseDto actual = rentalService.getById(RENTAL_ID, customer);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Manager gets a rental of another user by id")
    void getById_ManagerOnForeignRental_ReturnsRentalDto() {
        // Given
        User manager = createUser(MANAGER_ID, Role.MANAGER);
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Rental rental = createRental(customer, createCar(INVENTORY), null);
        RentalResponseDto expected = createRentalResponseDto();
        when(rentalRepository.findWithDetailsById(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(rentalMapper.toDto(rental)).thenReturn(expected);

        // When
        RentalResponseDto actual = rentalService.getById(RENTAL_ID, manager);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Customer getting a rental of another user is denied")
    void getById_CustomerOnForeignRental_ThrowsAccessDeniedException() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        User otherCustomer = createUser(OTHER_CUSTOMER_ID, Role.CUSTOMER);
        Rental rental = createRental(otherCustomer, createCar(INVENTORY), null);
        when(rentalRepository.findWithDetailsById(RENTAL_ID)).thenReturn(Optional.of(rental));

        // When
        assertThrows(AccessDeniedException.class,
                () -> rentalService.getById(RENTAL_ID, customer));

        // Then
        verify(rentalMapper, never()).toDto(any(Rental.class));
    }

    @Test
    @DisplayName("Get a non-existing rental by id throws an exception")
    void getById_NonExistingId_ThrowsEntityNotFoundException() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        when(rentalRepository.findWithDetailsById(NON_EXISTING_ID)).thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> rentalService.getById(NON_EXISTING_ID, customer));

        // Then
        assertEquals("Can't find rental by id: " + NON_EXISTING_ID, exception.getMessage());
    }

    @Test
    @DisplayName("Return a rental: actual return date set and car inventory incremented")
    void returnRental_ActiveOwnRental_SetsReturnDateAndIncrementsInventory() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Car car = createCar(INVENTORY);
        Rental rental = createRental(customer, car, null);
        RentalResponseDto expected = createRentalResponseDto();
        when(rentalRepository.findByIdForUpdate(RENTAL_ID)).thenReturn(Optional.of(rental));
        when(carRepository.findByIdForUpdate(CAR_ID)).thenReturn(Optional.of(car));
        when(rentalMapper.toDto(rental)).thenReturn(expected);

        // When
        RentalResponseDto actual = rentalService.returnRental(RENTAL_ID, customer);

        // Then
        assertEquals(expected, actual);
        assertEquals(LocalDate.now(), rental.getActualReturnDate());
        assertEquals(INVENTORY + 1, car.getInventory());
    }

    @Test
    @DisplayName("Return an already returned rental throws an exception")
    void returnRental_AlreadyReturned_ThrowsRentalAlreadyReturnedException() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        Car car = createCar(INVENTORY);
        Rental rental = createRental(customer, car, LocalDate.now().minusDays(1));
        when(rentalRepository.findByIdForUpdate(RENTAL_ID)).thenReturn(Optional.of(rental));

        // When
        assertThrows(RentalAlreadyReturnedException.class,
                () -> rentalService.returnRental(RENTAL_ID, customer));

        // Then
        assertEquals(INVENTORY, car.getInventory());
        verify(carRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Customer returning a rental of another user is denied")
    void returnRental_CustomerOnForeignRental_ThrowsAccessDeniedException() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        User otherCustomer = createUser(OTHER_CUSTOMER_ID, Role.CUSTOMER);
        Rental rental = createRental(otherCustomer, createCar(INVENTORY), null);
        when(rentalRepository.findByIdForUpdate(RENTAL_ID)).thenReturn(Optional.of(rental));

        // When
        assertThrows(AccessDeniedException.class,
                () -> rentalService.returnRental(RENTAL_ID, customer));

        // Then
        assertNull(rental.getActualReturnDate());
        verify(carRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Return a non-existing rental throws an exception")
    void returnRental_NonExistingId_ThrowsEntityNotFoundException() {
        // Given
        User customer = createUser(CUSTOMER_ID, Role.CUSTOMER);
        when(rentalRepository.findByIdForUpdate(NON_EXISTING_ID)).thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> rentalService.returnRental(NON_EXISTING_ID, customer));

        // Then
        assertEquals("Can't find rental by id: " + NON_EXISTING_ID, exception.getMessage());
    }

    @SuppressWarnings("unchecked")
    private RentalResponseDto stubFindAllWithOneRental(Pageable pageable) {
        Rental rental = createRental(createUser(CUSTOMER_ID, Role.CUSTOMER),
                createCar(INVENTORY), null);
        RentalResponseDto dto = createRentalResponseDto();
        when(rentalRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(rental), pageable, 1));
        when(rentalMapper.toDto(rental)).thenReturn(dto);
        return dto;
    }

    private User createUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setFirstName("First");
        user.setLastName("Last");
        user.setRole(role);
        return user;
    }

    private Car createCar(int inventory) {
        Car car = new Car();
        car.setId(CAR_ID);
        car.setModel("Corolla");
        car.setBrand("Toyota");
        car.setType(CarType.SEDAN);
        car.setInventory(inventory);
        car.setDailyFee(DAILY_FEE);
        return car;
    }

    private Rental createRental(User user, Car car, LocalDate actualReturnDate) {
        Rental rental = new Rental();
        rental.setId(RENTAL_ID);
        rental.setRentalDate(LocalDate.now().minusDays(2));
        rental.setReturnDate(LocalDate.now().plusDays(1));
        rental.setActualReturnDate(actualReturnDate);
        rental.setUser(user);
        rental.setCar(car);
        return rental;
    }

    private RentalResponseDto createRentalResponseDto() {
        return new RentalResponseDto(RENTAL_ID, LocalDate.now().minusDays(2),
                LocalDate.now().plusDays(1), null, CUSTOMER_ID, null);
    }
}
