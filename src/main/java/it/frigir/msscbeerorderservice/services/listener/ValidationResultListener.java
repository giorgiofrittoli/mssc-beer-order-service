package it.frigir.msscbeerorderservice.services.listener;


import it.frigir.brewery.model.events.ValidateBeerOrderResult;
import it.frigir.msscbeerorderservice.config.JmsConfig;
import it.frigir.msscbeerorderservice.services.BeerOrderManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationResultListener {

    private final BeerOrderManager beerOrderManager;

    @JmsListener(destination = JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE)
    public void validateBeerOrderResult(ValidateBeerOrderResult validateBeerOrderResult) {

        log.debug("Validation per beerOrder " + validateBeerOrderResult.getBeerOrderId() + " " + validateBeerOrderResult.getValid());

        beerOrderManager.processBeerOrderValidation(validateBeerOrderResult.getBeerOrderId(), validateBeerOrderResult.getValid());
    }

}
