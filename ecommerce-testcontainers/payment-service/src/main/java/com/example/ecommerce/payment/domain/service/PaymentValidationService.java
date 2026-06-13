package com.example.ecommerce.payment.domain.service;

import com.example.ecommerce.payment.domain.model.Payment;
import com.example.ecommerce.shared.domain.Money;

/**
 * Domain rules for whether a payment may be processed: amount sanity checks,
 * fraud-risk hints, etc. Kept tiny and side-effect free.
 */
public final class PaymentValidationService {

    public void validate(Payment payment) {
        Money amount = payment.amount();
        if (amount.amount().signum() <= 0) {
            throw new IllegalArgumentException("payment amount must be positive");
        }
    }
}
