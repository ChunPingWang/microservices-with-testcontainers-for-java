package com.example.ecommerce.payment.domain.port.inbound;

import com.example.ecommerce.payment.domain.model.PaymentId;
import com.example.ecommerce.payment.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProcessPaymentUseCase {

    PaymentId process(ProcessPaymentCommand command);

    record ProcessPaymentCommand(
            UUID orderId,
            String buyerId,
            String idempotencyKey,
            PaymentMethod method,
            BigDecimal amount,
            String currency) {}
}
