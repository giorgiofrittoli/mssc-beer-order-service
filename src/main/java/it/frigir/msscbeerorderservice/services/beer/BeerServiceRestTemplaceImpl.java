package it.frigir.msscbeerorderservice.services.beer;

import it.frigir.brewery.model.BeerDto;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@ConfigurationProperties(prefix = "sfg.brewery", ignoreInvalidFields = false)
@Component
public class BeerServiceRestTemplaceImpl implements BeerService {

    private final RestTemplate restTemplate;
    private final String BEER_PATH = "/api/v1/beerUpc/{upc}";
    private String beerServiceHost;

    public BeerServiceRestTemplaceImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public Optional<BeerDto> getBeer(String upc) {

        ResponseEntity<BeerDto> responseEntity = restTemplate.exchange(
                beerServiceHost + BEER_PATH,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<BeerDto>() {
                },
                upc
        );

        return Optional.of(responseEntity.getBody());

    }

    public void setBeerServiceHost(String beerServiceHost) {
        this.beerServiceHost = beerServiceHost;
    }
}
