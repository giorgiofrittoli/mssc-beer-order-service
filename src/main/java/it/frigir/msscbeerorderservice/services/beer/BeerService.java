package it.frigir.msscbeerorderservice.services.beer;

import it.frigir.brewery.model.BeerDto;

import java.util.Optional;

public interface BeerService {
    Optional<BeerDto> getBeer(String upc);
}
