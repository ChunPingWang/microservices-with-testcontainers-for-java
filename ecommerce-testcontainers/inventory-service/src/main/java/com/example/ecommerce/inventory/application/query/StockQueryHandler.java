package com.example.ecommerce.inventory.application.query;

import com.example.ecommerce.inventory.domain.model.SkuId;
import com.example.ecommerce.inventory.domain.port.inbound.QueryStockUseCase;
import com.example.ecommerce.inventory.domain.port.outbound.StockRepository;

public class StockQueryHandler implements QueryStockUseCase {

    private final StockRepository repository;

    public StockQueryHandler(StockRepository repository) {
        this.repository = repository;
    }

    @Override
    public StockView get(String sku) {
        return repository.findBySku(new SkuId(sku))
                .map(s -> {
                    var level = s.level();
                    return new StockView(sku, level.available().value(), level.reserved().value());
                })
                .orElse(new StockView(sku, 0, 0));
    }
}
