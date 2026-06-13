package com.example.ecommerce.product.adapter.inbound.kafka;

import com.example.ecommerce.product.domain.model.OrderId;
import com.example.ecommerce.product.domain.port.inbound.MarkOrderPaidUseCase;
import com.example.ecommerce.shared.event.PaymentCompletedIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Inbound (driving) adapter: reacts to PaymentCompleted events from the
 * payment context by updating the local order aggregate. Domain only sees
 * the inbound port — the consumer is just glue.
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final MarkOrderPaidUseCase markOrderPaid;

    public PaymentEventConsumer(MarkOrderPaidUseCase markOrderPaid) {
        this.markOrderPaid = markOrderPaid;
    }

    @KafkaListener(
            topics = PaymentCompletedIntegrationEvent.TOPIC,
            groupId = "${spring.kafka.consumer.group-id}")
    public void on(PaymentCompletedIntegrationEvent event) {
        log.info("PaymentCompleted received for order={}", event.orderId());
        try {
            markOrderPaid.markPaid(new OrderId(event.orderId()), event.paymentId());
        } catch (IllegalStateException ex) {
            // Already paid or in a non-payable state — treat as idempotent.
            log.warn("Ignoring PaymentCompleted for order={} : {}", event.orderId(), ex.getMessage());
        }
    }
}
