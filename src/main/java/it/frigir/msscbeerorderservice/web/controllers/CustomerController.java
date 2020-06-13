package it.frigir.msscbeerorderservice.web.controllers;

import it.frigir.brewery.model.CustomerPagedList;
import it.frigir.msscbeerorderservice.services.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/customers")
@RestController
public class CustomerController {


    private final CustomerService customerService;

    private static final Integer DEFAULT_PAGE_NUMBER = 0;
    private static final Integer DEFAULT_PAGE_SIZE = 25;


    @GetMapping
    public CustomerPagedList listCostumers(@RequestParam(value = "pageNumber", required = false) Integer pageNumber,
                                           @RequestParam(value = "pageSize", required = false) Integer pageSize) {

        if (pageNumber == null || pageNumber < 0) {
            pageNumber = DEFAULT_PAGE_NUMBER;
        }

        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        return customerService.listCostumers(PageRequest.of(pageNumber, pageSize));

    }

}
