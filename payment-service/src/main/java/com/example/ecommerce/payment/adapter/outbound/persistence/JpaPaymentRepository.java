package com.example.ecommerce.payment.adapter.outbound.persistence;

import com.example.ecommerce.payment.domain.model.IdempotencyKey;
import com.example.ecommerce.payment.domain.model.Payment;
import com.example.ecommerce.payment.domain.model.PaymentId;
import com.example.ecommerce.payment.domain.model.PaymentMethod;
import com.example.ecommerce.payment.domain.model.PaymentStatus;
import com.example.ecommerce.payment.domain.port.outbound.PaymentRepository;
import com.example.ecommerce.shared.domain.Money;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Optional;

@Repository
@Transactional
public class JpaPaymentRepository implements PaymentRepository {

    private final PaymentJpaRepository jpa;

    public JpaPaymentRepository(PaymentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = jpa.findById(payment.id().value()).orElseGet(PaymentJpaEntity::new);
        entity.setId(payment.id().value());
        entity.setOrderId(payment.orderId());
        entity.setBuyerId(payment.buyerId());
        entity.setIdempotencyKey(payment.idempotencyKey().value());
        entity.setMethod(payment.method().name());
        entity.setAmount(payment.amount().amount());
        entity.setCurrency(payment.amount().currency().getCurrencyCode());
        applyStatus(entity, payment.status());
        if (payment.version() > 0) entity.setVersion(payment.version());
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(PaymentId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByIdempotencyKey(IdempotencyKey key) {
        return jpa.findByIdempotencyKey(key.value()).map(this::toDomain);
    }

    private void applyStatus(PaymentJpaEntity e, PaymentStatus status) {
        e.setStatusAt(OffsetDateTime.ofInstant(status.at(), ZoneOffset.UTC));
        switch (status) {
            case PaymentStatus.Initiated __ -> {
                e.setStatus("INITIATED");
                e.setAuthCode(null);
                e.setReceiptUri(null);
                e.setFailureReason(null);
            }
            case PaymentStatus.Authorised a -> {
                e.setStatus("AUTHORISED");
                e.setAuthCode(a.authCode());
            }
            case PaymentStatus.Completed c -> {
                e.setStatus("COMPLETED");
                e.setReceiptUri(c.receiptUri());
            }
            case PaymentStatus.Failed f -> {
                e.setStatus("FAILED");
                e.setFailureReason(f.reason());
            }
            case PaymentStatus.Refunded r -> {
                e.setStatus("REFUNDED");
                e.setFailureReason(r.reason());
            }
        }
    }

    private Payment toDomain(PaymentJpaEntity e) {
        var amount = new Money(e.getAmount(), Currency.getInstance(e.getCurrency()));
        PaymentStatus status = toDomainStatus(e);
        return Payment.rehydrate(
                new PaymentId(e.getId()),
                e.getOrderId(),
                e.getBuyerId(),
                new IdempotencyKey(e.getIdempotencyKey()),
                PaymentMethod.valueOf(e.getMethod()),
                amount,
                status,
                e.getVersion());
    }

    private PaymentStatus toDomainStatus(PaymentJpaEntity e) {
        var at = e.getStatusAt().toInstant();
        return switch (e.getStatus()) {
            case "INITIATED" -> new PaymentStatus.Initiated(at);
            case "AUTHORISED" -> new PaymentStatus.Authorised(at, e.getAuthCode());
            case "COMPLETED" -> new PaymentStatus.Completed(at, e.getReceiptUri());
            case "FAILED" -> new PaymentStatus.Failed(at, e.getFailureReason());
            case "REFUNDED" -> new PaymentStatus.Refunded(at, e.getFailureReason());
            default -> throw new IllegalStateException("unknown payment status: " + e.getStatus());
        };
    }
}
