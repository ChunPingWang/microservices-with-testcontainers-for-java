package com.example.ecommerce.product.domain.model;

import java.util.UUID;

public record ProductId(UUID value) {

    public static ProductId newId() {
        return new ProductId(UUID.randomUUID());
    }

    public static ProductId of(String value) {
        return new ProductId(UUID.fromString(value));
    }
}
