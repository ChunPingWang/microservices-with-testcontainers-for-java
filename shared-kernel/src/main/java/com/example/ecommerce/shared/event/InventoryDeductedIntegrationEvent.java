package com.example.ecommerce.shared.event;

import java.time.Instant;
import java.util.UUID;

public record InventoryDeductedIntegrationEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        String sku,
        int quantity
) implements IntegrationEvent {

    public static final String TOPIC = "inventory.deducted";

    @Override
    public String topic() {
        return TOPIC;
    }
}
