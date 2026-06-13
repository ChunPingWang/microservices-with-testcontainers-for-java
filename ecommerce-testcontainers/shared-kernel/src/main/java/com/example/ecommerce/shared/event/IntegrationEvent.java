package com.example.ecommerce.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker for cross-context events flowing over the message bus.
 * Each implementation MUST be a Java record so that it is immutable and
 * trivially serialisable.
 */
public sealed interface IntegrationEvent
        permits OrderCreatedIntegrationEvent,
                PaymentCompletedIntegrationEvent,
                PaymentFailedIntegrationEvent,
                InventoryDeductedIntegrationEvent,
                InventoryDeductionFailedIntegrationEvent {

    UUID eventId();

    Instant occurredAt();

    /** Topic / channel name the publisher should route to. */
    String topic();
}
