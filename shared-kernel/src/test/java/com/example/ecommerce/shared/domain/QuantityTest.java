package com.example.ecommerce.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {

    @Test
    void cannot_be_negative() {
        assertThatThrownBy(() -> Quantity.of(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void add_and_subtract() {
        assertThat(Quantity.of(5).add(Quantity.of(3)).value()).isEqualTo(8);
        assertThat(Quantity.of(5).subtract(Quantity.of(3)).value()).isEqualTo(2);
    }

    @Test
    void subtract_below_zero_throws() {
        assertThatThrownBy(() -> Quantity.of(2).subtract(Quantity.of(5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void less_than() {
        assertThat(Quantity.of(3).lessThan(Quantity.of(5))).isTrue();
        assertThat(Quantity.of(5).lessThan(Quantity.of(3))).isFalse();
    }
}
