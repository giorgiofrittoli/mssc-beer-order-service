package it.frigir.msscbeerorderservice.services;

import it.frigir.brewery.model.BeerOrderDto;
import it.frigir.msscbeerorderservice.domain.BeerOrder;

import java.util.UUID;

public interface BeerOrderManager {

    BeerOrder newBeerOrder(BeerOrder beerOrder);

    void processBeerOrderValidation(UUID beerOrderId, Boolean valid);

    void processBeerOrderAllocation(BeerOrderDto beerOrderDto, Boolean allocationError, Boolean pendingInventory);

}
