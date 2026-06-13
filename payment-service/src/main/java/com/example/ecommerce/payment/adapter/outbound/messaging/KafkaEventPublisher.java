package com.example.ecommerce.payment.adapter.outbound.messaging;

import com.example.ecommerce.shared.event.IntegrationEvent;
import com.example.ecommerce.shared.port.EventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> template;

    public KafkaEventPublisher(KafkaTemplate<String, Object> template) {
        this.template = template;
    }

    @Override
    public void publish(IntegrationEvent event) {
        template.send(event.topic(), event.eventId().toString(), event);
    }
}
