package mate.academy.carsharingservice.service;

import mate.academy.carsharingservice.dto.CarDto;
import mate.academy.carsharingservice.dto.CreateCarRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CarService {
    CarDto create(CreateCarRequestDto request);

    Page<CarDto> getAll(Pageable pageable);

    CarDto getById(Long id);

    CarDto update(Long id, CreateCarRequestDto request);

    void deleteById(Long id);
}
