package com.example.ecommerce.payment.domain;

import com.example.ecommerce.payment.domain.model.IdempotencyKey;
import com.example.ecommerce.payment.domain.model.Payment;
import com.example.ecommerce.payment.domain.model.PaymentMethod;
import com.example.ecommerce.payment.domain.model.PaymentStatus;
import com.example.ecommerce.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private static final Clock FIXED = Clock.fixed(
            Instant.parse("2026-06-13T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void initiate_then_authorise_then_complete() {
        Payment p = Payment.initiate(UUID.randomUUID(), "buyer-1",
                new IdempotencyKey("k-1"), PaymentMethod.CREDIT_CARD,
                Money.of("50.00", "USD"), FIXED);
        assertThat(p.status()).isInstanceOf(PaymentStatus.Initiated.class);

        p.authorise("AUTH-XYZ", FIXED);
        assertThat(p.status()).isInstanceOfSatisfying(PaymentStatus.Authorised.class,
                s -> assertThat(s.authCode()).isEqualTo("AUTH-XYZ"));

        p.complete("s3://receipts/x", FIXED);
        assertThat(p.status()).isInstanceOf(PaymentStatus.Completed.class);
    }

    @Test
    void cannot_complete_before_authorise() {
        Payment p = Payment.initiate(UUID.randomUUID(), "buyer-1",
                new IdempotencyKey("k-2"), PaymentMethod.CREDIT_CARD,
                Money.of("50.00", "USD"), FIXED);
        assertThatThrownBy(() -> p.complete("uri", FIXED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fail_can_happen_from_initiated_or_authorised() {
        Payment p = Payment.initiate(UUID.randomUUID(), "buyer", new IdempotencyKey("k-3"),
                PaymentMethod.CREDIT_CARD, Money.of("10.00", "USD"), FIXED);
        p.fail("declined", FIXED);
        assertThat(p.status()).isInstanceOf(PaymentStatus.Failed.class);
    }

    @Test
    void cannot_fail_completed_payment() {
        Payment p = Payment.initiate(UUID.randomUUID(), "buyer", new IdempotencyKey("k-4"),
                PaymentMethod.CREDIT_CARD, Money.of("10.00", "USD"), FIXED);
        p.authorise("AUTH", FIXED);
        p.complete("uri", FIXED);
        assertThatThrownBy(() -> p.fail("nope", FIXED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refund_only_from_completed() {
        Payment p = Payment.initiate(UUID.randomUUID(), "buyer", new IdempotencyKey("k-5"),
                PaymentMethod.CREDIT_CARD, Money.of("10.00", "USD"), FIXED);
        assertThatThrownBy(() -> p.refund("chargeback", FIXED))
                .isInstanceOf(IllegalStateException.class);

        p.authorise("AUTH", FIXED);
        p.complete("uri", FIXED);
        p.refund("chargeback", FIXED);
        assertThat(p.status()).isInstanceOf(PaymentStatus.Refunded.class);
    }

    @Test
    void idempotency_key_validates_length() {
        assertThatThrownBy(() -> new IdempotencyKey(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IdempotencyKey("x".repeat(200)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
