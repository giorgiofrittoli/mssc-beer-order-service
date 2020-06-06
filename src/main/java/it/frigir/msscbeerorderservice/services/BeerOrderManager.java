package it.frigir.msscbeerorderservice.services;

import it.frigir.msscbeerorderservice.domain.BeerOrder;
import it.frigir.msscbeerorderservice.domain.BeerOrderEventEnum;
import it.frigir.msscbeerorderservice.domain.BeerOrderStatusEnum;
import org.springframework.statemachine.StateMachine;

import java.util.UUID;

public interface BeerOrderManager {

    BeerOrder newBeerOrder(BeerOrder beerOrder);
    StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> processBeerOrderValidation(UUID beerOrderId, Boolean valid);
}
