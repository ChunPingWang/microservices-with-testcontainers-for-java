package com.example.ecommerce.shared.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedIntegrationEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String buyerId,
        BigDecimal totalAmount,
        String currency,
        List<Line> lines
) implements IntegrationEvent {

    public static final String TOPIC = "order.created";

    @Override
    public String topic() {
        return TOPIC;
    }

    public record Line(String sku, int quantity, BigDecimal unitPrice) {}
}
