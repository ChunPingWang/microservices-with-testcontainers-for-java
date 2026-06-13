package com.example.ecommerce.product.domain;

import com.example.ecommerce.product.domain.service.PricingService;
import com.example.ecommerce.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    @Test
    void apply_tax_increases_total_by_rate() {
        PricingService svc = new PricingService(new BigDecimal("0.05"));
        Money taxed = svc.applyTax(Money.of("100.00", "USD"));
        assertThat(taxed.amount()).isEqualByComparingTo("105.00");
    }

    @Test
    void volume_discount_kicks_in_above_threshold() {
        PricingService svc = new PricingService(BigDecimal.ZERO);
        assertThat(svc.applyVolumeDiscount(Money.of("99.00", "USD")).amount())
                .isEqualByComparingTo("99.00");
        assertThat(svc.applyVolumeDiscount(Money.of("200.00", "USD")).amount())
                .isEqualByComparingTo("180.00");
    }
}
