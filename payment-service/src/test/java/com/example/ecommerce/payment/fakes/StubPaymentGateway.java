package com.example.ecommerce.payment.fakes;

import com.example.ecommerce.payment.domain.model.PaymentMethod;
import com.example.ecommerce.payment.domain.port.outbound.PaymentGateway;
import com.example.ecommerce.shared.domain.Money;

import java.math.BigDecimal;

public class StubPaymentGateway implements PaymentGateway {

    private final BigDecimal failOver;

    public StubPaymentGateway(BigDecimal failOver) {
        this.failOver = failOver;
    }

    @Override
    public AuthorisationResult authorise(PaymentMethod method, Money amount, String idempotencyKey) {
        if (amount.amount().compareTo(failOver) >= 0) {
            return AuthorisationResult.failure("over ceiling");
        }
        return AuthorisationResult.success("STUB-AUTH");
    }
}
