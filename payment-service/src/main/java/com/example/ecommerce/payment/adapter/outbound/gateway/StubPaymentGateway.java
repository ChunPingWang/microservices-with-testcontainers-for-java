package com.example.ecommerce.payment.adapter.outbound.gateway;

import com.example.ecommerce.payment.domain.model.PaymentMethod;
import com.example.ecommerce.payment.domain.port.outbound.PaymentGateway;
import com.example.ecommerce.shared.domain.Money;
import com.example.ecommerce.shared.port.SecretProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Default 3rd-party payment gateway adapter. Reads the API key from
 * {@link SecretProvider} (Vault in prod, properties in dev/test) to prove
 * the Hexagonal swap-ability of the secret source.
 *
 * <p>The stub authorises any amount under the configured ceiling — perfect
 * for tests; a real adapter would call Stripe/Adyen here.
 */
@Component
public class StubPaymentGateway implements PaymentGateway {

    private final SecretProvider secretProvider;
    private final BigDecimal failOver;

    public StubPaymentGateway(SecretProvider secretProvider,
                              @Value("${payment.fail-over-amount:10000.00}") BigDecimal failOver) {
        this.secretProvider = secretProvider;
        this.failOver = failOver;
    }

    @Override
    public AuthorisationResult authorise(PaymentMethod method, Money amount, String idempotencyKey) {
        // Touch the secret provider so behaviour is observable when Vault is wired.
        secretProvider.get("payment.api-key");

        if (amount.amount().compareTo(failOver) >= 0) {
            return AuthorisationResult.failure("amount exceeds gateway ceiling");
        }
        return AuthorisationResult.success("AUTH-" + UUID.randomUUID());
    }
}
