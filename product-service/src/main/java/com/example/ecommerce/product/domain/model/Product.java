package com.example.ecommerce.product.domain.model;

import com.example.ecommerce.shared.domain.Money;

import java.util.Objects;

public final class Product {

    private final ProductId id;
    private final String sku;
    private String name;
    private String description;
    private Money price;
    private boolean active;

    public Product(ProductId id, String sku, String name, String description, Money price, boolean active) {
        this.id = Objects.requireNonNull(id);
        this.sku = Objects.requireNonNull(sku);
        this.name = Objects.requireNonNull(name);
        this.description = description == null ? "" : description;
        this.price = Objects.requireNonNull(price);
        this.active = active;
        if (price.isNegative()) {
            throw new IllegalArgumentException("price cannot be negative");
        }
    }

    public ProductId id() { return id; }
    public String sku() { return sku; }
    public String name() { return name; }
    public String description() { return description; }
    public Money price() { return price; }
    public boolean isActive() { return active; }

    public void rename(String newName) {
        this.name = Objects.requireNonNull(newName);
    }

    public void changePrice(Money newPrice) {
        Objects.requireNonNull(newPrice);
        if (newPrice.isNegative()) {
            throw new IllegalArgumentException("price cannot be negative");
        }
        this.price = newPrice;
    }

    public void deactivate() {
        this.active = false;
    }
}
