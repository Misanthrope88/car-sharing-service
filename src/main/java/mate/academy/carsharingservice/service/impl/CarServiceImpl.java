package mate.academy.carsharingservice.service.impl;

import lombok.RequiredArgsConstructor;
import mate.academy.carsharingservice.dto.CarDto;
import mate.academy.carsharingservice.dto.CreateCarRequestDto;
import mate.academy.carsharingservice.exception.EntityNotFoundException;
import mate.academy.carsharingservice.mapper.CarMapper;
import mate.academy.carsharingservice.model.Car;
import mate.academy.carsharingservice.repository.CarRepository;
import mate.academy.carsharingservice.service.CarService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {
    private final CarRepository carRepository;
    private final CarMapper carMapper;

    @Override
    @Transactional
    public CarDto create(CreateCarRequestDto request) {
        Car car = carMapper.toEntity(request);
        return carMapper.toDto(carRepository.save(car));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarDto> getAll(Pageable pageable) {
        return carRepository.findAll(pageable)
                .map(carMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CarDto getById(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Can't find car by id: " + id));
        return carMapper.toDto(car);
    }

    @Override
    @Transactional
    public CarDto update(Long id, CreateCarRequestDto request) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Can't find car by id: " + id));
        carMapper.updateCarFromDto(request, car);
        return carMapper.toDto(carRepository.save(car));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Can't find car by id: " + id));
        carRepository.delete(car);
    }
}
