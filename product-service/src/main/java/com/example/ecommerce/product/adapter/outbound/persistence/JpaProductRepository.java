package com.example.ecommerce.product.adapter.outbound.persistence;

import com.example.ecommerce.product.domain.model.Product;
import com.example.ecommerce.product.domain.model.ProductId;
import com.example.ecommerce.product.domain.port.outbound.ProductRepository;
import com.example.ecommerce.shared.domain.Money;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class JpaProductRepository implements ProductRepository {

    private final ProductJpaRepository jpa;

    public JpaProductRepository(ProductJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = jpa.findById(product.id().value()).orElseGet(ProductJpaEntity::new);
        entity.setId(product.id().value());
        entity.setSku(product.sku());
        entity.setName(product.name());
        entity.setDescription(product.description());
        entity.setPriceAmount(product.price().amount());
        entity.setPriceCurrency(product.price().currency().getCurrencyCode());
        entity.setActive(product.isActive());
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(ProductId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findBySku(String sku) {
        return jpa.findBySku(sku).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    private Product toDomain(ProductJpaEntity e) {
        Money price = new Money(e.getPriceAmount(), Currency.getInstance(e.getPriceCurrency()));
        return new Product(new ProductId(e.getId()), e.getSku(), e.getName(),
                e.getDescription(), price, e.isActive());
    }
}
