package com.example.ecommerce.shared.domain;

public record Quantity(int value) {

    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("quantity cannot be negative: " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public static Quantity zero() {
        return new Quantity(0);
    }

    public Quantity add(Quantity other) {
        return new Quantity(value + other.value);
    }

    public Quantity subtract(Quantity other) {
        return new Quantity(value - other.value);
    }

    public boolean lessThan(Quantity other) {
        return value < other.value;
    }

    public boolean isZero() {
        return value == 0;
    }
}
