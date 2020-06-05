package it.frigir.msscbeerorderservice.services;

import it.frigir.msscbeerorderservice.domain.BeerOrder;

public interface BeerOrderManager {

    BeerOrder newBeerOrder(BeerOrder beerOrder);

}
