package it.frigir.msscbeerorderservice.services.listener;

import it.frigir.brewery.model.events.AllocateOrderResult;
import it.frigir.msscbeerorderservice.config.JmsConfig;
import it.frigir.msscbeerorderservice.services.BeerOrderManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AllocationResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE)
    public void allocateOrderResult(AllocateOrderResult allocateOrderResult) {

        beerOrderManager.processBeerOrderAllocation(allocateOrderResult.getBeerOrderDto(),
                allocateOrderResult.getAllocationError(),
                allocateOrderResult.getPendingInventory());

    }

}
