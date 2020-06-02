package it.frigir.msscbeerorderservice.services.beer;

import it.frigir.msscbeerorderservice.services.beer.model.BeerDto;

import java.util.Optional;

public interface BeerService {
    Optional<BeerDto> getBeer(String upc);
}
