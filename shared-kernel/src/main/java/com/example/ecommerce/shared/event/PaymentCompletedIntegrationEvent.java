package com.example.ecommerce.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedIntegrationEvent(
        UUID eventId,
        Instant occurredAt,
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        String currency,
        String receiptUri
) implements IntegrationEvent {

    public static final String TOPIC = "payment.completed";

    @Override
    public String topic() {
        return TOPIC;
    }
}
