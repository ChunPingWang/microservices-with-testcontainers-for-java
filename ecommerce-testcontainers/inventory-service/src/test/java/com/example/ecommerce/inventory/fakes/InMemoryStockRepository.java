package com.example.ecommerce.inventory.fakes;

import com.example.ecommerce.inventory.domain.model.SkuId;
import com.example.ecommerce.inventory.domain.model.Stock;
import com.example.ecommerce.inventory.domain.port.outbound.StockRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStockRepository implements StockRepository {

    private final Map<SkuId, Stock> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Stock> findBySku(SkuId sku) {
        return Optional.ofNullable(store.get(sku));
    }

    @Override
    public Stock save(Stock stock) {
        store.put(stock.sku(), stock);
        return stock;
    }

    public void seed(SkuId sku, int initial) {
        store.put(sku, Stock.seed(sku, initial));
    }
}
