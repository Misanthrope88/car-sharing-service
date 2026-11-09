package mate.academy.carsharingservice.service;

import mate.academy.carsharingservice.dto.CreateRentalRequestDto;
import mate.academy.carsharingservice.dto.RentalResponseDto;
import mate.academy.carsharingservice.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RentalService {
    RentalResponseDto create(CreateRentalRequestDto request, User user);

    Page<RentalResponseDto> getAll(User user, Long userId, Boolean isActive, Pageable pageable);

    RentalResponseDto getById(Long id, User user);

    RentalResponseDto returnRental(Long id, User user);
}
