package it.frigir.msscbeerorderservice.services;

import it.frigir.brewery.model.CustomerPagedList;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    CustomerPagedList listCostumers(Pageable pageable);

}
