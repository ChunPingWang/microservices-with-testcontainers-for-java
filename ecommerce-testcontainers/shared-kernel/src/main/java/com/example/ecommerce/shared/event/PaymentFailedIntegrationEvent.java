package com.example.ecommerce.shared.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedIntegrationEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String reason
) implements IntegrationEvent {

    public static final String TOPIC = "payment.failed";

    @Override
    public String topic() {
        return TOPIC;
    }
}
