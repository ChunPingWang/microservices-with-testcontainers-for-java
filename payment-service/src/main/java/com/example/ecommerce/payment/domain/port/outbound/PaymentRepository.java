package com.example.ecommerce.payment.domain.port.outbound;

import com.example.ecommerce.payment.domain.model.IdempotencyKey;
import com.example.ecommerce.payment.domain.model.Payment;
import com.example.ecommerce.payment.domain.model.PaymentId;

import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(PaymentId id);

    Optional<Payment> findByIdempotencyKey(IdempotencyKey key);
}
