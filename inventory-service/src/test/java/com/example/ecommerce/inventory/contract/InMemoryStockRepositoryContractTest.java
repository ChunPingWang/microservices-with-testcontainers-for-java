package com.example.ecommerce.inventory.contract;

import com.example.ecommerce.inventory.domain.port.outbound.StockRepository;
import com.example.ecommerce.inventory.fakes.InMemoryStockRepository;

class InMemoryStockRepositoryContractTest extends StockRepositoryContract {

    private final InMemoryStockRepository repo = new InMemoryStockRepository();

    @Override
    protected StockRepository repository() {
        return repo;
    }
}
