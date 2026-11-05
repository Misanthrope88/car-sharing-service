package mate.academy.carsharingservice.service;

import mate.academy.carsharingservice.dto.CreateRentalRequestDto;
import mate.academy.carsharingservice.dto.RentalResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RentalService {
    RentalResponseDto create(CreateRentalRequestDto request);

    Page<RentalResponseDto> getAll(Pageable pageable);

    RentalResponseDto getById(Long id);
}
