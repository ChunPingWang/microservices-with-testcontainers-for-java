package com.example.ecommerce.payment.domain.port.outbound;

import com.example.ecommerce.payment.domain.model.PaymentMethod;
import com.example.ecommerce.shared.domain.Money;

/**
 * External 3rd-party payment processor. Real adapter would talk to Stripe /
 * Adyen; in tests we use a stub or simulate failures.
 */
public interface PaymentGateway {

    AuthorisationResult authorise(PaymentMethod method, Money amount, String idempotencyKey);

    record AuthorisationResult(boolean success, String authCode, String failureReason) {

        public static AuthorisationResult success(String authCode) {
            return new AuthorisationResult(true, authCode, null);
        }

        public static AuthorisationResult failure(String reason) {
            return new AuthorisationResult(false, null, reason);
        }
    }
}
