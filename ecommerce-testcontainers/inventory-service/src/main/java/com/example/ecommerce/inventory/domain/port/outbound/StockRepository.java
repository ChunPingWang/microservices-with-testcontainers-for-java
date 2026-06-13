package com.example.ecommerce.inventory.domain.port.outbound;

import com.example.ecommerce.inventory.domain.model.SkuId;
import com.example.ecommerce.inventory.domain.model.Stock;

import java.util.Optional;

public interface StockRepository {
    Optional<Stock> findBySku(SkuId sku);
    Stock save(Stock stock);
}
