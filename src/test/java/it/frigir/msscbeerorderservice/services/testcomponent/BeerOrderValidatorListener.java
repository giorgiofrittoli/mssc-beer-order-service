package it.frigir.msscbeerorderservice.services.testcomponent;

import it.frigir.brewery.model.events.ValidateBeerOrderRequest;
import it.frigir.brewery.model.events.ValidateBeerOrderResult;
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
public class BeerOrderValidatorListener {

    private final JmsTemplate jmsTemplate;

    //mock jms listener for validation
    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void listen(Message msg) {

        ValidateBeerOrderRequest validateBeerOrderRequest = (ValidateBeerOrderRequest) msg.getPayload();

        log.debug("Got test validation order " + validateBeerOrderRequest);

        jmsTemplate.convertAndSend(JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
                ValidateBeerOrderResult.builder()
                        .valid(!validateBeerOrderRequest.getBeerOrder().getCustomerRef().equals(BeerOrderManagerIT.FAIL_VALIDATION_CUSTOM_REF))
                        .beerOrderId(validateBeerOrderRequest.getBeerOrder().getId()).build()
        );
    }

}
