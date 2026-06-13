package com.example.ecommerce.inventory.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface StockJpaRepository extends JpaRepository<StockJpaEntity, String> {
}
