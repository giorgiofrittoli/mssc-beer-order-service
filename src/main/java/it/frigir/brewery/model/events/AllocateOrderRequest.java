package it.frigir.brewery.model.events;

import it.frigir.brewery.model.BeerOrderDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AllocateOrderRequest {
    private BeerOrderDto beerOrder;
}
