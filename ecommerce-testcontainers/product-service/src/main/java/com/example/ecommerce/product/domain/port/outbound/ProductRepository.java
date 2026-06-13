package com.example.ecommerce.product.domain.port.outbound;

import com.example.ecommerce.product.domain.model.Product;
import com.example.ecommerce.product.domain.model.ProductId;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(ProductId id);
    Optional<Product> findBySku(String sku);
    List<Product> findAll();
}
