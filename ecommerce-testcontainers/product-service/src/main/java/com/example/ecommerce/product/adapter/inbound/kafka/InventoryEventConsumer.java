package com.example.ecommerce.product.adapter.inbound.kafka;

import com.example.ecommerce.product.domain.model.OrderId;
import com.example.ecommerce.product.domain.port.inbound.CompleteOrderUseCase;
import com.example.ecommerce.shared.event.InventoryDeductedIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Closes the loop: once inventory has been deducted, the order is "completed".
 */
@Component
public class InventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    private final CompleteOrderUseCase completeOrder;

    public InventoryEventConsumer(CompleteOrderUseCase completeOrder) {
        this.completeOrder = completeOrder;
    }

    @KafkaListener(
            topics = InventoryDeductedIntegrationEvent.TOPIC,
            groupId = "${spring.kafka.consumer.group-id}")
    public void on(InventoryDeductedIntegrationEvent event) {
        log.info("InventoryDeducted received for order={}", event.orderId());
        try {
            completeOrder.complete(new OrderId(event.orderId()));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            log.warn("Ignoring InventoryDeducted for order={}: {}", event.orderId(), ex.getMessage());
        }
    }
}
