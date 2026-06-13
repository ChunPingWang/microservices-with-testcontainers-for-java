package com.example.ecommerce.product.domain.service;

import com.example.ecommerce.shared.domain.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure domain service — no Spring annotation here.
 * Applies tax + discount rules to a subtotal.
 */
public final class PricingService {

    private final BigDecimal taxRate; // e.g. 0.05 for 5%

    public PricingService(BigDecimal taxRate) {
        if (taxRate.signum() < 0) {
            throw new IllegalArgumentException("taxRate cannot be negative");
        }
        this.taxRate = taxRate;
    }

    public Money applyTax(Money subtotal) {
        BigDecimal taxed = subtotal.amount().multiply(BigDecimal.ONE.add(taxRate))
                .setScale(subtotal.currency().getDefaultFractionDigits(), RoundingMode.HALF_UP);
        return new Money(taxed, subtotal.currency());
    }

    /** Threshold-based simple discount: 10% off for orders > $100. */
    public Money applyVolumeDiscount(Money subtotal) {
        BigDecimal threshold = BigDecimal.valueOf(100);
        if (subtotal.amount().compareTo(threshold) > 0) {
            BigDecimal discounted = subtotal.amount().multiply(new BigDecimal("0.90"))
                    .setScale(subtotal.currency().getDefaultFractionDigits(), RoundingMode.HALF_UP);
            return new Money(discounted, subtotal.currency());
        }
        return subtotal;
    }
}
