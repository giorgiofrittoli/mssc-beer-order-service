package it.frigir.msscbeerorderservice.services;

import it.frigir.brewery.model.BeerOrderDto;
import it.frigir.msscbeerorderservice.domain.BeerOrder;
import it.frigir.msscbeerorderservice.domain.BeerOrderEventEnum;
import it.frigir.msscbeerorderservice.domain.BeerOrderStatusEnum;
import it.frigir.msscbeerorderservice.repositories.BeerOrderRepository;
import it.frigir.msscbeerorderservice.sm.BeerOrderStateChangeInterceptor;
import it.frigir.msscbeerorderservice.web.mappers.BeerOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {

    public static String BEER_ORDER_ID_HEADER = "beer_order_id";

    private final BeerOrderRepository beerOrderRepository;
    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;
    private final BeerOrderMapper beerOrderMapper;

    @Transactional
    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {

        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

        BeerOrder savedBeerOrder = beerOrderRepository.save(beerOrder);
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);

        return savedBeerOrder;
    }

    @Transactional
    @Override
    public void processBeerOrderValidation(UUID beerOrderId, Boolean valid) {

        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderId);

        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            if (valid) {

                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);

                awaitForStatus(beerOrderId, BeerOrderStatusEnum.VALIDATED);

                BeerOrder validatedOrder = beerOrderRepository.findById(beerOrderId).get();

                sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_ORDER);

            } else {
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
            }
        }, () -> log.error("Beer order not found " + beerOrderId));
    }

    @Transactional
    @Override
    public void processBeerOrderAllocation(BeerOrderDto beerOrderDto,
                                           Boolean allocationError,
                                           Boolean pendingInventory) {

        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderDto.getId());

        beerOrderOptional.ifPresentOrElse(beerOrder -> {
            if (allocationError) {
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED);
            } else if (pendingInventory) {

                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);

                awaitForStatus(beerOrderDto.getId(),BeerOrderStatusEnum.ALLOCATION_PENDING);

                updateAllocatedQty(beerOrderDto, beerOrder);
            } else {

                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);

                awaitForStatus(beerOrderDto.getId(),BeerOrderStatusEnum.ALLOCATED);

                updateAllocatedQty(beerOrderDto, beerOrder);
            }

        }, () -> log.error("Beer order not found " + beerOrderDto));

    }

    @Transactional
    @Override
    public void pickUpOrder(UUID beerOrderId) {

        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderId);

        beerOrderOptional.ifPresentOrElse(
                beerOrder -> sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.BEER_ORDER_PICKED_UP),
                () -> log.error("Beer order not found " + beerOrderId)
        );

    }

    @Transactional
    @Override
    public void cancelOrder(UUID beerOrderId) {

        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById(beerOrderId);

        beerOrderOptional.ifPresentOrElse(
                beerOrder -> sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.CANCEL_ORDER),
                () -> log.error("Beer order not found " + beerOrderId)
        );

    }

    // await for sm to set the desired status
    private void awaitForStatus(UUID beerOrderId, BeerOrderStatusEnum statusEnum) {

        AtomicBoolean found = new AtomicBoolean(false);
        AtomicInteger loopCount = new AtomicInteger(0);

        while (!found.get()) {

            if (loopCount.incrementAndGet() > 10) {
                found.set(true);
                log.debug("Loop retries exceeded");
            }

            beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
                if (beerOrder.getOrderStatus().equals(statusEnum)) {
                    found.set(true);
                    log.debug("Order found");
                } else {
                    log.debug("Order status not equal. Expected " + statusEnum.name() + " found " + beerOrder.getOrderStatus().name());
                }
            }, () -> log.error("Beer order not found"));

            if (!found.get()) {
                try {
                    log.debug("Wating fo state change");
                    Thread.sleep(100);
                } catch (Exception e) {

                }
            }
        }

    }

    private void updateAllocatedQty(BeerOrderDto beerOrderDto, BeerOrder beerOrder) {

        beerOrder.getBeerOrderLines().forEach(beerOrderLine -> {
            beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                if (beerOrderLineDto.getId().equals(beerOrderLine.getId())) {
                    beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                }
            });
        });

        beerOrderRepository.saveAndFlush(beerOrder);

    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {

        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = stateMachineFactory.getStateMachine(beerOrder.getId());
        sm.stop();

        sm.getStateMachineAccessor()
                .doWithAllRegions(
                        sma -> {
                            sma.addStateMachineInterceptor(beerOrderStateChangeInterceptor);
                            sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null));
                        }
                );

        sm.start();

        return sm;

    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum) {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build(beerOrder);

        Message msg = MessageBuilder.withPayload(eventEnum).setHeader(BEER_ORDER_ID_HEADER, beerOrder.getId().toString()).build();

        sm.sendEvent(msg);

    }

}
