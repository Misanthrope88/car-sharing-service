package mate.academy.carsharingservice.mapper;

import mate.academy.carsharingservice.dto.RentalResponseDto;
import mate.academy.carsharingservice.model.Rental;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = CarMapper.class)
public interface RentalMapper {
    @Mapping(target = "userId", source = "user.id")
    RentalResponseDto toDto(Rental rental);
}
