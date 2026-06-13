package com.example.ecommerce.shared.event;

import java.time.Instant;
import java.util.UUID;

public record InventoryDeductionFailedIntegrationEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String reason
) implements IntegrationEvent {

    public static final String TOPIC = "inventory.deduction-failed";

    @Override
    public String topic() {
        return TOPIC;
    }
}
