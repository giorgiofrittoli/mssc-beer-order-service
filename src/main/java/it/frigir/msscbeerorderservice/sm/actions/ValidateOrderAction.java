package it.frigir.msscbeerorderservice.sm.actions;

import it.frigir.brewery.model.events.ValidateBeerOrderRequest;
import it.frigir.msscbeerorderservice.config.JmsConfig;
import it.frigir.msscbeerorderservice.domain.BeerOrder;
import it.frigir.msscbeerorderservice.domain.BeerOrderEventEnum;
import it.frigir.msscbeerorderservice.domain.BeerOrderStatusEnum;
import it.frigir.msscbeerorderservice.repositories.BeerOrderRepository;
import it.frigir.msscbeerorderservice.services.BeerOrderManagerImpl;
import it.frigir.msscbeerorderservice.web.mappers.BeerOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class ValidateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {

        Optional.ofNullable(stateContext.getMessage())
                .ifPresent(msg -> {
                    UUID orderId = UUID.fromString((String) msg.getHeaders().getOrDefault(BeerOrderManagerImpl.BEER_ORDER_ID_HEADER, " "));
                    BeerOrder beerOrder = beerOrderRepository.findById(orderId).get();
                    jmsTemplate.convertAndSend(
                            JmsConfig.VALIDATE_ORDER_QUEUE,
                            new ValidateBeerOrderRequest(beerOrderMapper.beerOrderToDto(beerOrder)));
                });
    }
}
