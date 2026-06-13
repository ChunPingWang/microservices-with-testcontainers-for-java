package com.example.ecommerce.product.domain.model;

import com.example.ecommerce.shared.domain.Money;
import com.example.ecommerce.shared.domain.Quantity;

public record OrderLine(String sku, Quantity quantity, Money unitPrice) {

    public OrderLine {
        if (quantity.isZero()) {
            throw new IllegalArgumentException("order line quantity must be positive");
        }
        if (unitPrice.isNegative()) {
            throw new IllegalArgumentException("unit price cannot be negative");
        }
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity.value());
    }
}
