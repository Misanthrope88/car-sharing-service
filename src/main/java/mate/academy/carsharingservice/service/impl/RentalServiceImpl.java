package mate.academy.carsharingservice.service.impl;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import mate.academy.carsharingservice.dto.CreateRentalRequestDto;
import mate.academy.carsharingservice.dto.RentalResponseDto;
import mate.academy.carsharingservice.exception.CarUnavailableException;
import mate.academy.carsharingservice.exception.EntityNotFoundException;
import mate.academy.carsharingservice.mapper.RentalMapper;
import mate.academy.carsharingservice.model.Car;
import mate.academy.carsharingservice.model.Rental;
import mate.academy.carsharingservice.model.Role;
import mate.academy.carsharingservice.model.User;
import mate.academy.carsharingservice.repository.CarRepository;
import mate.academy.carsharingservice.repository.RentalRepository;
import mate.academy.carsharingservice.repository.UserRepository;
import mate.academy.carsharingservice.service.RentalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {
    private final RentalRepository rentalRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final RentalMapper rentalMapper;

    @Override
    @Transactional
    public RentalResponseDto create(CreateRentalRequestDto request) {
        final User user = getCurrentUser();
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
        return rentalMapper.toDto(rentalRepository.save(rental));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RentalResponseDto> getAll(Long userId, Boolean isActive, Pageable pageable) {
        User user = getCurrentUser();
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
    public RentalResponseDto getById(Long id) {
        User user = getCurrentUser();
        Rental rental = rentalRepository.findWithDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Can't find rental by id: " + id));
        if (user.getRole() != Role.MANAGER && !rental.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        return rentalMapper.toDto(rental);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new InsufficientAuthenticationException("Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }
}
