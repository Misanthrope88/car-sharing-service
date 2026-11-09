package mate.academy.carsharingservice.service.impl;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import mate.academy.carsharingservice.dto.CreateRentalRequestDto;
import mate.academy.carsharingservice.dto.RentalResponseDto;
import mate.academy.carsharingservice.exception.CarUnavailableException;
import mate.academy.carsharingservice.exception.EntityNotFoundException;
import mate.academy.carsharingservice.exception.RentalAlreadyReturnedException;
import mate.academy.carsharingservice.mapper.RentalMapper;
import mate.academy.carsharingservice.model.Car;
import mate.academy.carsharingservice.model.Rental;
import mate.academy.carsharingservice.model.Role;
import mate.academy.carsharingservice.model.User;
import mate.academy.carsharingservice.repository.CarRepository;
import mate.academy.carsharingservice.repository.RentalRepository;
import mate.academy.carsharingservice.service.RentalService;
import mate.academy.carsharingservice.service.notification.NotificationMessageBuilder;
import mate.academy.carsharingservice.service.notification.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {
    private final RentalRepository rentalRepository;
    private final CarRepository carRepository;
    private final RentalMapper rentalMapper;
    private final NotificationService notificationService;
    private final NotificationMessageBuilder messageBuilder;

    @Override
    @Transactional
    public RentalResponseDto create(CreateRentalRequestDto request, User user) {
        Car car = carRepository.findActiveByIdForUpdate(request.carId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find car by id: " + request.carId()));
        if (car.getInventory() <= 0) {
            throw new CarUnavailableException("Car is not available for rental");
        }
        Rental rental = new Rental();
        rental.setRentalDate(LocalDate.now());
        rental.setReturnDate(request.returnDate());
        rental.setActualReturnDate(null);
        rental.setCar(car);
        rental.setUser(user);
        car.setInventory(car.getInventory() - 1);
        Rental savedRental = rentalRepository.save(rental);
        notificationService.sendMessage(messageBuilder.buildNewRentalMessage(savedRental));
        return rentalMapper.toDto(savedRental);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RentalResponseDto> getAll(
            User user, Long userId, Boolean isActive, Pageable pageable) {
        if (user.getRole() != Role.MANAGER && userId != null) {
            throw new AccessDeniedException("User filter requires manager role");
        }
        Long effectiveUserId = user.getRole() == Role.MANAGER ? userId : user.getId();
        Specification<Rental> specification = (root, query, builder) -> builder.conjunction();
        if (effectiveUserId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("user").get("id"), effectiveUserId));
        }
        if (isActive != null) {
            specification = specification.and((root, query, builder) -> isActive
                    ? builder.isNull(root.get("actualReturnDate"))
                    : builder.isNotNull(root.get("actualReturnDate")));
        }
        return rentalRepository.findAll(specification, pageable).map(rentalMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalResponseDto getById(Long id, User user) {
        Rental rental = rentalRepository.findWithDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Can't find rental by id: " + id));
        if (user.getRole() != Role.MANAGER && !rental.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        return rentalMapper.toDto(rental);
    }

    @Override
    @Transactional
    public RentalResponseDto returnRental(Long id, User user) {
        Rental rental = rentalRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Can't find rental by id: " + id));
        if (user.getRole() != Role.MANAGER && !rental.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        if (rental.getActualReturnDate() != null) {
            throw new RentalAlreadyReturnedException(
                    "Rental with id " + id + " is already returned");
        }
        Car car = carRepository.findByIdForUpdate(rental.getCar().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find car by id: " + rental.getCar().getId()));
        car.setInventory(car.getInventory() + 1);
        rental.setActualReturnDate(LocalDate.now());
        return rentalMapper.toDto(rental);
    }
}
