package it.frigir.msscbeerorderservice.services.testcomponent;

import it.frigir.brewery.model.events.AllocateOrderRequest;
import it.frigir.brewery.model.events.AllocateOrderResult;
import it.frigir.msscbeerorderservice.config.JmsConfig;
import it.frigir.msscbeerorderservice.services.BeerOrderManagerIT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderAllocationListener {
    private final JmsTemplate jmsTemplate;

    //mock jms listener for allocation
    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(Message msg) {

        AllocateOrderRequest allocateOrderRequest = (AllocateOrderRequest) msg.getPayload();

        log.debug("Got test allocation order " + allocateOrderRequest);


        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
                AllocateOrderResult.builder()
                        .allocationError(allocateOrderRequest.getBeerOrder().getCustomerRef().equals(BeerOrderManagerIT.FAIL_ALLOCATION_CUSTOM_REF))
                        .pendingInventory(allocateOrderRequest.getBeerOrder().getCustomerRef().equals(BeerOrderManagerIT.ALLOCATION_PENDING_CUSTOM_REF))
                        .beerOrderDto(allocateOrderRequest.getBeerOrder()).build()
        );
    }
}
