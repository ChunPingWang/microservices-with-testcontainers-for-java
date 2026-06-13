package com.example.ecommerce.inventory.domain.model;

import java.util.Objects;

public record SkuId(String value) {

    public SkuId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || value.length() > 64) {
            throw new IllegalArgumentException("SkuId must be 1..64 chars");
        }
    }
}
