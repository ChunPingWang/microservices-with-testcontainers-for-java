package com.example.ecommerce.inventory.domain.model;

import com.example.ecommerce.shared.domain.Quantity;

import java.util.Objects;
import java.util.UUID;

/**
 * Soft hold on a quantity of stock pending payment confirmation.
 * Identified by {@code orderId} so a duplicate reserve(orderId, qty) call
 * is a no-op.
 */
public record Reservation(UUID orderId, Quantity quantity) {

    public Reservation {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(quantity, "quantity");
        if (quantity.isZero()) {
            throw new IllegalArgumentException("reservation quantity must be positive");
        }
    }
}
