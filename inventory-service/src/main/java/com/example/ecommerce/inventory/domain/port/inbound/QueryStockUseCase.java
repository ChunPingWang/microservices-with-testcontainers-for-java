package com.example.ecommerce.inventory.domain.port.inbound;

public interface QueryStockUseCase {

    StockView get(String sku);

    record StockView(String sku, int available, int reserved) {}
}
