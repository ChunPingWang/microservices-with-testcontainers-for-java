package com.example.ecommerce.product.domain.port.inbound;

import java.math.BigDecimal;
import java.util.List;

public interface SearchProductUseCase {

    List<ProductView> search(SearchQuery query);

    record SearchQuery(String keyword, int page, int size) {}

    record ProductView(String id, String sku, String name, BigDecimal price, String currency, boolean active) {}
}
