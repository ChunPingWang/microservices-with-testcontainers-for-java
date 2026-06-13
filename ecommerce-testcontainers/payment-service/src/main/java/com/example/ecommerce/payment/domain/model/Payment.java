package com.example.ecommerce.payment.domain.model;

import com.example.ecommerce.shared.domain.Money;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Payment {

    private final PaymentId id;
    private final UUID orderId;
    private final String buyerId;
    private final IdempotencyKey idempotencyKey;
    private final PaymentMethod method;
    private final Money amount;
    private PaymentStatus status;
    private long version;

    private Payment(PaymentId id, UUID orderId, String buyerId,
                    IdempotencyKey idempotencyKey, PaymentMethod method,
                    Money amount, PaymentStatus status, long version) {
        this.id = Objects.requireNonNull(id);
        this.orderId = Objects.requireNonNull(orderId);
        this.buyerId = Objects.requireNonNull(buyerId);
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.method = Objects.requireNonNull(method);
        this.amount = Objects.requireNonNull(amount);
        this.status = Objects.requireNonNull(status);
        this.version = version;
        if (amount.isNegative()) {
            throw new IllegalArgumentException("payment amount cannot be negative");
        }
    }

    public static Payment initiate(UUID orderId, String buyerId, IdempotencyKey key,
                                   PaymentMethod method, Money amount, Clock clock) {
        return new Payment(PaymentId.newId(), orderId, buyerId, key, method, amount,
                new PaymentStatus.Initiated(clock.instant()), 0L);
    }

    public static Payment rehydrate(PaymentId id, UUID orderId, String buyerId,
                                    IdempotencyKey key, PaymentMethod method, Money amount,
                                    PaymentStatus status, long version) {
        return new Payment(id, orderId, buyerId, key, method, amount, status, version);
    }

    public void authorise(String authCode, Clock clock) {
        if (!(status instanceof PaymentStatus.Initiated)) {
            throw new IllegalStateException("only Initiated payments can be authorised; current=" + status);
        }
        this.status = new PaymentStatus.Authorised(clock.instant(), authCode);
    }

    public void complete(String receiptUri, Clock clock) {
        if (!(status instanceof PaymentStatus.Authorised)) {
            throw new IllegalStateException("only Authorised payments can be completed; current=" + status);
        }
        this.status = new PaymentStatus.Completed(clock.instant(), receiptUri);
    }

    public void fail(String reason, Clock clock) {
        if (status instanceof PaymentStatus.Completed || status instanceof PaymentStatus.Refunded) {
            throw new IllegalStateException("cannot fail a completed/refunded payment");
        }
        this.status = new PaymentStatus.Failed(clock.instant(), reason);
    }

    public void refund(String reason, Clock clock) {
        if (!(status instanceof PaymentStatus.Completed)) {
            throw new IllegalStateException("only Completed payments can be refunded; current=" + status);
        }
        this.status = new PaymentStatus.Refunded(clock.instant(), reason);
    }

    public PaymentId id() { return id; }
    public UUID orderId() { return orderId; }
    public String buyerId() { return buyerId; }
    public IdempotencyKey idempotencyKey() { return idempotencyKey; }
    public PaymentMethod method() { return method; }
    public Money amount() { return amount; }
    public PaymentStatus status() { return status; }
    public long version() { return version; }

    public Instant occurredAt() { return status.at(); }
}
