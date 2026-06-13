package com.example.ecommerce.payment.fakes;

import com.example.ecommerce.payment.domain.model.IdempotencyKey;
import com.example.ecommerce.payment.domain.model.Payment;
import com.example.ecommerce.payment.domain.model.PaymentId;
import com.example.ecommerce.payment.domain.port.outbound.PaymentRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<PaymentId, Payment> store = new ConcurrentHashMap<>();
    private final Map<IdempotencyKey, PaymentId> byKey = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        store.put(payment.id(), payment);
        byKey.put(payment.idempotencyKey(), payment.id());
        return payment;
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(IdempotencyKey key) {
        return Optional.ofNullable(byKey.get(key)).map(store::get);
    }
}
