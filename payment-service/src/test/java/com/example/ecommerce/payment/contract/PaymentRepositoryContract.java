package com.example.ecommerce.payment.contract;

import com.example.ecommerce.payment.domain.model.IdempotencyKey;
import com.example.ecommerce.payment.domain.model.Payment;
import com.example.ecommerce.payment.domain.model.PaymentId;
import com.example.ecommerce.payment.domain.model.PaymentMethod;
import com.example.ecommerce.payment.domain.model.PaymentStatus;
import com.example.ecommerce.payment.domain.port.outbound.PaymentRepository;
import com.example.ecommerce.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioural contract that every implementation of {@link PaymentRepository}
 * must satisfy. Two concrete subclasses extend this and supply the actual
 * adapter from {@link #repository()} — {@code InMemoryPaymentRepository} for
 * the unit-test fake and {@code JpaPaymentRepository} for the real adapter.
 *
 * <p>If both run green, the two implementations are truly substitutable
 * (Liskov), so application-layer tests written against the in-memory fake
 * can be trusted to match production behaviour.
 */
public abstract class PaymentRepositoryContract {

    private static final Clock FIXED = Clock.fixed(
            Instant.parse("2026-06-13T00:00:00Z"), ZoneOffset.UTC);

    protected abstract PaymentRepository repository();

    @Test
    void save_then_load_by_id() {
        Payment p = Payment.initiate(UUID.randomUUID(), "buyer-1",
                new IdempotencyKey("contract-1"), PaymentMethod.CREDIT_CARD,
                Money.of("50.00", "USD"), FIXED);
        repository().save(p);

        Payment loaded = repository().findById(p.id()).orElseThrow();
        assertThat(loaded.id()).isEqualTo(p.id());
        assertThat(loaded.buyerId()).isEqualTo("buyer-1");
        assertThat(loaded.amount().amount()).isEqualByComparingTo("50.00");
        assertThat(loaded.status()).isInstanceOf(PaymentStatus.Initiated.class);
    }

    @Test
    void find_by_idempotency_key_returns_existing_payment() {
        IdempotencyKey key = new IdempotencyKey("contract-idem");
        Payment p = Payment.initiate(UUID.randomUUID(), "buyer-2", key,
                PaymentMethod.WALLET, Money.of("12.50", "USD"), FIXED);
        repository().save(p);

        assertThat(repository().findByIdempotencyKey(key)).isPresent()
                .get().extracting(Payment::id).isEqualTo(p.id());
    }

    @Test
    void find_by_unknown_idempotency_key_is_empty() {
        assertThat(repository().findByIdempotencyKey(new IdempotencyKey("never-seen")))
                .isEmpty();
    }

    @Test
    void status_transition_authorised_then_completed_is_persisted() {
        Payment p = Payment.initiate(UUID.randomUUID(), "buyer-3",
                new IdempotencyKey("contract-status"), PaymentMethod.CREDIT_CARD,
                Money.of("80.00", "USD"), FIXED);
        p = repository().save(p);
        p.authorise("AUTH-XYZ", FIXED);
        p.complete("s3://receipts/x.pdf", FIXED);
        repository().save(p);

        Payment loaded = repository().findById(p.id()).orElseThrow();
        assertThat(loaded.status()).isInstanceOfSatisfying(
                PaymentStatus.Completed.class,
                c -> assertThat(c.receiptUri()).isEqualTo("s3://receipts/x.pdf"));
    }

    @Test
    void find_by_unknown_id_is_empty() {
        assertThat(repository().findById(PaymentId.newId())).isEmpty();
    }
}
