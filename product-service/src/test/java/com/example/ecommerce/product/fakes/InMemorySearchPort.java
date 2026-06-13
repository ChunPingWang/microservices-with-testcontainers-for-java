package com.example.ecommerce.product.fakes;

import com.example.ecommerce.product.domain.port.inbound.SearchProductUseCase;
import com.example.ecommerce.product.domain.port.outbound.SearchPort;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySearchPort implements SearchPort {

    private final Map<String, SearchProductUseCase.ProductView> index = new ConcurrentHashMap<>();

    @Override
    public void index(SearchProductUseCase.ProductView view) {
        index.put(view.id(), view);
    }

    @Override
    public void remove(String productId) {
        index.remove(productId);
    }

    @Override
    public List<SearchProductUseCase.ProductView> search(SearchProductUseCase.SearchQuery query) {
        String kw = query.keyword() == null ? "" : query.keyword().toLowerCase(Locale.ROOT);
        return index.values().stream()
                .filter(v -> kw.isEmpty()
                        || v.name().toLowerCase(Locale.ROOT).contains(kw)
                        || v.sku().toLowerCase(Locale.ROOT).contains(kw))
                .sorted(Comparator.comparing(SearchProductUseCase.ProductView::sku))
                .skip((long) query.page() * query.size())
                .limit(query.size())
                .toList();
    }

    public void clear() {
        index.clear();
    }
}
