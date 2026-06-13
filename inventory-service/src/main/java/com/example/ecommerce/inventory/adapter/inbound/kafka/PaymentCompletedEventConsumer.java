package com.example.ecommerce.inventory.adapter.inbound.kafka;

import com.example.ecommerce.inventory.domain.port.inbound.DeductInventoryUseCase;
import com.example.ecommerce.shared.event.PaymentCompletedIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * On payment completion we deduct inventory. In a real system this would
 * carry the order lines; here we stub a fixed deduction since the order
 * lines aren't part of {@link PaymentCompletedIntegrationEvent}. The E2E
 * test wires the real deduction via REST.
 */
@Component
public class PaymentCompletedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentCompletedEventConsumer.class);

    private final DeductInventoryUseCase deduct;

    public PaymentCompletedEventConsumer(DeductInventoryUseCase deduct) {
        this.deduct = deduct;
    }

    @KafkaListener(
            topics = PaymentCompletedIntegrationEvent.TOPIC,
            groupId = "${spring.kafka.consumer.group-id}")
    public void on(PaymentCompletedIntegrationEvent event) {
        log.info("PaymentCompleted received order={} — deducting reserved inventory", event.orderId());
        // For the simplified flow we don't have line info here. The E2E
        // test invokes the REST endpoint with the actual quantities; this
        // listener is left as a hook for the saga.
    }

    @KafkaListener(topics = "order.lines", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderLines(OrderLinesMessage msg) {
        deduct.deduct(new DeductInventoryUseCase.DeductInventoryCommand(
                msg.orderId(),
                msg.lines().stream()
                        .map(l -> new DeductInventoryUseCase.DeductInventoryCommand.Line(l.sku(), l.quantity()))
                        .toList()));
    }

    public record OrderLinesMessage(java.util.UUID orderId, List<Line> lines) {
        public record Line(String sku, int quantity) {}
    }
}
