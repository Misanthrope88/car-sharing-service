package mate.academy.carsharingservice.mapper;

import mate.academy.carsharingservice.dto.PaymentDto;
import mate.academy.carsharingservice.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {
    @Mapping(target = "rentalId", source = "rental.id")
    PaymentDto toDto(Payment payment);
}
