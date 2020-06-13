package it.frigir.msscbeerorderservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import it.frigir.brewery.model.BeerDto;
import it.frigir.brewery.model.events.FailedAllocation;
import it.frigir.msscbeerorderservice.config.JmsConfig;
import it.frigir.msscbeerorderservice.domain.BeerOrder;
import it.frigir.msscbeerorderservice.domain.BeerOrderLine;
import it.frigir.msscbeerorderservice.domain.BeerOrderStatusEnum;
import it.frigir.msscbeerorderservice.domain.Customer;
import it.frigir.msscbeerorderservice.repositories.BeerOrderRepository;
import it.frigir.msscbeerorderservice.repositories.CustomerRepository;
import it.frigir.msscbeerorderservice.services.beer.BeerServiceRestTemplateImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(WireMockExtension.class)
@SpringBootTest
public class BeerOrderManagerIT {

    public static final String FAIL_VALIDATION_CUSTOM_REF = "fail-validation";
    public static final String ALLOCATION_PENDING_CUSTOM_REF = "allocation-pending";
    public static final String FAIL_ALLOCATION_CUSTOM_REF = "fail-allocation";
    public static final String CANCEL_ALLOCATION_CUSTOM_REF = "cancel-allocation";

    @Autowired
    BeerOrderManager beerOrderManager;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    Customer testCustomer;

    UUID beerId = UUID.randomUUID();

    @Autowired
    WireMockServer wireMockServer;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JmsTemplate jmsTemplate;

    final String UPC = "12345";

    @TestConfiguration
    static class RestTemplateProvider {

        @Bean(destroyMethod = "stop")
        public WireMockServer wireMockServer() {
            WireMockServer wireMockServer = with(wireMockConfig().port(8083));
            wireMockServer.start();
            return wireMockServer;
        }

    }

    @BeforeEach
    void setUp() {
        testCustomer = customerRepository.save(Customer.builder()
                .customerName("Test costumer")
                .build());

        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
    }

    @Test
    void newToAllocated() throws JsonProcessingException, InterruptedException {

        BeerDto beerDto = BeerDto.builder().id(beerId).upc(UPC).build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_UPC_PATH_V1 + beerDto.getUpc())
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
        });

        BeerOrder validatedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertNotNull(validatedBeerOrder);
        assertEquals(BeerOrderStatusEnum.ALLOCATED, validatedBeerOrder.getOrderStatus());

    }

    @Test
    void newToPickedUp() throws JsonProcessingException {

        BeerDto beerDto = BeerDto.builder().id(beerId).upc(UPC).build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_UPC_PATH_V1 + beerDto.getUpc())
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
        });

        beerOrderManager.pickUpOrder(savedBeerOrder.getId());

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.PICKED_UP, foundOrder.getOrderStatus());
        });

        BeerOrder pickedUpBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertNotNull(pickedUpBeerOrder);
        assertEquals(BeerOrderStatusEnum.PICKED_UP, pickedUpBeerOrder.getOrderStatus());

    }

    @Test
    void newToFailedValidation() throws JsonProcessingException {

        BeerDto beerDto = BeerDto.builder().id(beerId).upc(UPC).build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_UPC_PATH_V1 + beerDto.getUpc())
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder(FAIL_VALIDATION_CUSTOM_REF);

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.VALIDATION_EXCEPTION, foundOrder.getOrderStatus());
        });

        BeerOrder failedValidationBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertNotNull(failedValidationBeerOrder);
        assertEquals(BeerOrderStatusEnum.VALIDATION_EXCEPTION, failedValidationBeerOrder.getOrderStatus());

    }

    @Test
    void newToFailedAllocation() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc(UPC).build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_UPC_PATH_V1 + beerDto.getUpc())
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder(FAIL_ALLOCATION_CUSTOM_REF);

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATION_EXCEPTION, foundOrder.getOrderStatus());
        });

        BeerOrder failedAllocationBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertNotNull(failedAllocationBeerOrder);
        assertEquals(BeerOrderStatusEnum.ALLOCATION_EXCEPTION, failedAllocationBeerOrder.getOrderStatus());


        FailedAllocation failedAllocation = (FailedAllocation) jmsTemplate.receiveAndConvert(JmsConfig.FAILED_ALLOCATION_QUEUE);

        assertNotNull(failedAllocationBeerOrder);
        assertEquals(failedAllocationBeerOrder.getId(), failedAllocation.getBeerOrderId());
    }

    @Test
    void newToAllocationPending() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc(UPC).build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_UPC_PATH_V1 + beerDto.getUpc())
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder(ALLOCATION_PENDING_CUSTOM_REF);

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATION_PENDING, foundOrder.getOrderStatus());
        });

        BeerOrder failedAllocationBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertNotNull(failedAllocationBeerOrder);
        assertEquals(BeerOrderStatusEnum.ALLOCATION_PENDING, failedAllocationBeerOrder.getOrderStatus());
    }

    @Test
    void allocationPendingToCanceled() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc(UPC).build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_UPC_PATH_V1 + beerDto.getUpc())
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder(CANCEL_ALLOCATION_CUSTOM_REF);

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        beerOrderManager.cancelOrder(savedBeerOrder.getId());

        BeerOrder canceledBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertNotNull(canceledBeerOrder);
        assertEquals(BeerOrderStatusEnum.CANCELLED, canceledBeerOrder.getOrderStatus());

    }

    @Test
    void allocatedToCanceled() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder().id(beerId).upc(UPC).build();

        wireMockServer.stubFor(get(BeerServiceRestTemplateImpl.BEER_UPC_PATH_V1 + beerDto.getUpc())
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        await().untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATED, foundOrder.getOrderStatus());
        });

        beerOrderManager.cancelOrder(savedBeerOrder.getId());

        BeerOrder canceledBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertNotNull(canceledBeerOrder);
        assertEquals(BeerOrderStatusEnum.CANCELLED, canceledBeerOrder.getOrderStatus());
    }

    public BeerOrder createBeerOrder() {
        return createBeerOrder("");
    }

    public BeerOrder createBeerOrder(String customRef) {

        BeerOrder beerOrder = BeerOrder.builder()
                .customer(testCustomer)
                .customerRef(customRef)
                .build();

        Set<BeerOrderLine> lines = new HashSet<>();
        lines.add(BeerOrderLine.builder()
                .beerId(beerId)
                .upc(UPC)
                .orderQuantity(1)
                .beerOrder(beerOrder)
                .build());

        beerOrder.setBeerOrderLines(lines);

        return beerOrder;
    }

}
