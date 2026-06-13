package com.example.ecommerce.product.adapter.outbound.search;

import com.example.ecommerce.product.domain.port.inbound.SearchProductUseCase.ProductView;
import com.example.ecommerce.product.domain.port.inbound.SearchProductUseCase.SearchQuery;
import com.example.ecommerce.product.domain.port.outbound.SearchPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnBean(ElasticsearchOperations.class)
public class ElasticsearchSearchAdapter implements SearchPort {

    private static final IndexCoordinates INDEX = IndexCoordinates.of("products");

    private final ElasticsearchOperations es;

    public ElasticsearchSearchAdapter(ElasticsearchOperations es) {
        this.es = es;
        ensureIndex();
    }

    private void ensureIndex() {
        IndexOperations ops = es.indexOps(ProductDocument.class);
        if (!ops.exists()) {
            ops.create();
            ops.putMapping(ops.createMapping(ProductDocument.class));
        }
    }

    @Override
    public void index(ProductView view) {
        ProductDocument doc = new ProductDocument();
        doc.id = view.id();
        doc.sku = view.sku();
        doc.name = view.name();
        doc.price = view.price().doubleValue();
        doc.currency = view.currency();
        doc.active = view.active();
        es.save(doc, INDEX);
        es.indexOps(INDEX).refresh();
    }

    @Override
    public void remove(String productId) {
        es.delete(productId, INDEX);
        es.indexOps(INDEX).refresh();
    }

    @Override
    public List<ProductView> search(SearchQuery query) {
        Criteria criteria;
        if (query.keyword() == null || query.keyword().isBlank()) {
            criteria = new Criteria(); // matchAll
        } else {
            criteria = new Criteria("name").contains(query.keyword())
                    .or(new Criteria("sku").contains(query.keyword()));
        }
        CriteriaQuery cq = new CriteriaQuery(criteria, PageRequest.of(query.page(), query.size()));
        SearchHits<ProductDocument> hits = es.search(cq, ProductDocument.class, INDEX);
        return hits.getSearchHits().stream()
                .map(h -> h.getContent().toView())
                .toList();
    }
}
