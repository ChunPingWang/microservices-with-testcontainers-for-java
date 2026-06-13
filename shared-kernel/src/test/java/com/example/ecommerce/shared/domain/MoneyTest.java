package com.example.ecommerce.shared.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void of_rounds_to_currency_default_fraction_digits() {
        Money m = Money.of("12.345", "USD");
        assertThat(m.amount()).isEqualByComparingTo("12.35");
    }

    @Test
    void add_same_currency_sums() {
        Money a = Money.of("10.00", "USD");
        Money b = Money.of("2.50", "USD");
        assertThat(a.add(b).amount()).isEqualByComparingTo("12.50");
    }

    @Test
    void add_different_currency_throws() {
        Money usd = Money.of("10.00", "USD");
        Money twd = Money.of("100", "TWD");
        assertThatThrownBy(() -> usd.add(twd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency mismatch");
    }

    @Test
    void multiply_by_quantity() {
        assertThat(Money.of("3.50", "USD").multiply(4).amount())
                .isEqualByComparingTo("14.00");
    }

    @Test
    void zero_has_correct_scale() {
        Money zero = Money.zero(Currency.getInstance("JPY"));
        assertThat(zero.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(zero.amount().scale()).isEqualTo(0); // JPY has no fraction
    }

    @Test
    void negative_detection() {
        assertThat(Money.of("-1.00", "USD").isNegative()).isTrue();
        assertThat(Money.of("0.00", "USD").isNegative()).isFalse();
    }
}
