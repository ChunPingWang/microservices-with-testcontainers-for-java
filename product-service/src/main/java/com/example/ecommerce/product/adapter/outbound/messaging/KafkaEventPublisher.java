package com.example.ecommerce.product.adapter.outbound.messaging;

import com.example.ecommerce.shared.event.IntegrationEvent;
import com.example.ecommerce.shared.port.EventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Sends each integration event to a Kafka topic named after
 * {@link IntegrationEvent#topic()}. Key = event id so duplicate
 * deliveries land on the same partition.
 */
@Component
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(IntegrationEvent event) {
        kafkaTemplate.send(event.topic(), event.eventId().toString(), event);
    }
}
