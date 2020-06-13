package it.frigir.msscbeerorderservice.services;

import it.frigir.brewery.model.CustomerPagedList;
import it.frigir.msscbeerorderservice.domain.Customer;
import it.frigir.msscbeerorderservice.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerPagedList listCostumers(Pageable pageable) {

        Page<Customer> customerDtoPage =
                customerRepository.findAll(pageable);

        return new CustomerPagedList(customerDtoPage
                .stream()
                .map(customerMapper::toDto)
                .collect(Collectors.toList()), PageRequest.of(
                customerDtoPage.getPageable().getPageNumber(),
                customerDtoPage.getPageable().getPageSize()),
                customerDtoPage.getTotalElements());

    }
}
