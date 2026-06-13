package com.example.ecommerce.product.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {
    Optional<ProductJpaEntity> findBySku(String sku);
}
