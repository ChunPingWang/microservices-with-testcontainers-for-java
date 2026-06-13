package com.example.ecommerce.inventory.domain.model;

import com.example.ecommerce.shared.domain.Quantity;

public record StockLevel(Quantity available, Quantity reserved) {

    public StockLevel {
        if (available == null || reserved == null) {
            throw new IllegalArgumentException("available and reserved are required");
        }
    }

    public Quantity onHand() {
        return available.add(reserved);
    }

    public static StockLevel of(int available, int reserved) {
        return new StockLevel(Quantity.of(available), Quantity.of(reserved));
    }
}
