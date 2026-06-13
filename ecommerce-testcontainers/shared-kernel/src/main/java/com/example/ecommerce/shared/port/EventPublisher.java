package com.example.ecommerce.shared.port;

import com.example.ecommerce.shared.event.IntegrationEvent;

/**
 * Outbound port for publishing integration events to the message bus.
 * Real adapter: Kafka. Test adapter: in-memory. Alternate: GCP Pub/Sub.
 */
public interface EventPublisher {

    void publish(IntegrationEvent event);
}
