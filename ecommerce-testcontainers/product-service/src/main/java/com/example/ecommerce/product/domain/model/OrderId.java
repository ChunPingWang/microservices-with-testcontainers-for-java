package com.example.ecommerce.product.domain.model;

import java.util.UUID;

public record OrderId(UUID value) {

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }
}
