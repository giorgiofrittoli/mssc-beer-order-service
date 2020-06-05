package it.frigir.msscbeerorderservice.web.mappers;


import it.frigir.msscbeerorderservice.domain.BeerOrderLine;
import it.frigir.msscbeerorderservice.services.beer.BeerService;
import it.frigir.brewery.model.BeerOrderLineDto;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BeerOrderLineDecorator implements BeerOrderLineMapper {

    private BeerOrderLineMapper beerOrderLineMapper;
    private BeerService beerService;

    @Override
    public BeerOrderLineDto beerOrderLineToDto(BeerOrderLine line) {

        BeerOrderLineDto beerOrderLineDto = beerOrderLineMapper.beerOrderLineToDto(line);

        beerService.getBeer(beerOrderLineDto.getUpc()).ifPresent(beerDto -> {
            beerOrderLineDto.setBeerName(beerDto.getBeerName());
            beerOrderLineDto.setBeerStyle(beerDto.getBeerStyle().toString());
            beerOrderLineDto.setBeerId(beerDto.getId());
        });

        return beerOrderLineDto;
    }

    @Override
    public BeerOrderLine dtoToBeerOrderLine(BeerOrderLineDto dto) {
        return beerOrderLineMapper.dtoToBeerOrderLine(dto);
    }

    //to inject generated mapper
    @Autowired
    public void setBeerOrderLineMapper(BeerOrderLineMapper beerOrderLineMapper) {
        this.beerOrderLineMapper = beerOrderLineMapper;
    }

    //to inject service
    @Autowired
    public void setBeerService(BeerService beerService) {
        this.beerService = beerService;
    }
}
