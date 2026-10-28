package mate.academy.carsharingservice.mapper;

import mate.academy.carsharingservice.dto.CarDto;
import mate.academy.carsharingservice.dto.CreateCarRequestDto;
import mate.academy.carsharingservice.model.Car;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CarMapper {
    CarDto toDto(Car car);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Car toEntity(CreateCarRequestDto request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateCarFromDto(CreateCarRequestDto request, @MappingTarget Car car);
}
