package it.frigir.msscbeerorderservice.services;

import it.frigir.brewery.model.CustomerDto;
import it.frigir.msscbeerorderservice.domain.Customer;
import it.frigir.msscbeerorderservice.web.mappers.DateMapper;
import org.mapstruct.Mapper;


@Mapper(uses = {DateMapper.class})
public interface CustomerMapper {
    CustomerDto toDto(Customer customer);
    Customer toDomain(CustomerDto customerDto);
}
