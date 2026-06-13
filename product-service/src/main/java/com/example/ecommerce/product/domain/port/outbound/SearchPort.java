package com.example.ecommerce.product.domain.port.outbound;

import com.example.ecommerce.product.domain.port.inbound.SearchProductUseCase.ProductView;
import com.example.ecommerce.product.domain.port.inbound.SearchProductUseCase.SearchQuery;

import java.util.List;

public interface SearchPort {
    void index(ProductView view);
    void remove(String productId);
    List<ProductView> search(SearchQuery query);
}
