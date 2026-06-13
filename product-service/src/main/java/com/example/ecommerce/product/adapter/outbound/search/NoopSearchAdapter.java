package com.example.ecommerce.product.adapter.outbound.search;

import com.example.ecommerce.product.domain.port.inbound.SearchProductUseCase;
import com.example.ecommerce.product.domain.port.outbound.SearchPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Used when Elasticsearch is not on the classpath / not configured. Lets the
 * service still expose its REST surface in dev / E2E without ES; results are
 * always empty.
 */
@Component
@ConditionalOnMissingBean(value = SearchPort.class, ignored = NoopSearchAdapter.class)
public class NoopSearchAdapter implements SearchPort {

    @Override
    public void index(SearchProductUseCase.ProductView view) {}

    @Override
    public void remove(String productId) {}

    @Override
    public List<SearchProductUseCase.ProductView> search(SearchProductUseCase.SearchQuery query) {
        return List.of();
    }
}
