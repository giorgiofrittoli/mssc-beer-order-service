package it.frigir.msscbeerorderservice.sm;

import it.frigir.msscbeerorderservice.domain.BeerOrder;
import it.frigir.msscbeerorderservice.domain.BeerOrderEventEnum;
import it.frigir.msscbeerorderservice.domain.BeerOrderStatusEnum;
import it.frigir.msscbeerorderservice.repositories.BeerOrderRepository;
import it.frigir.msscbeerorderservice.services.BeerOrderManagerImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class BeerOrderStateChangeInterceptor extends StateMachineInterceptorAdapter<BeerOrderStatusEnum, BeerOrderEventEnum> {

    private final BeerOrderRepository beerOrderRepository;

    @Override
    public void preStateChange(State<BeerOrderStatusEnum, BeerOrderEventEnum> state,
                               Message<BeerOrderEventEnum> message, Transition<BeerOrderStatusEnum, BeerOrderEventEnum> transition,
                               StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine) {

        Optional.ofNullable(message)
                .ifPresent(msg -> {
                    Optional.ofNullable(
                            UUID.fromString((String) msg.getHeaders().getOrDefault(BeerOrderManagerImpl.BEER_ORDER_ID_HEADER, "")))
                            .ifPresent(beerOrderId -> {
                                BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderId);
                                beerOrder.setOrderStatus(state.getId());
                                beerOrderRepository.saveAndFlush(beerOrder);
                            });
                });
    }
}
