package com.example.ecommerce.payment.domain.model;

import java.util.Objects;

/**
 * Caller-supplied identifier ensuring the same payment is never charged twice.
 * Typically derived from order id + buyer id; uniqueness is enforced at the
 * repository layer via a database UNIQUE constraint.
 */
public record IdempotencyKey(String value) {

    public IdempotencyKey {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("IdempotencyKey must be 1..128 chars, got: " + value);
        }
    }
}
