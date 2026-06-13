package com.example.ecommerce.payment.adapter.inbound.kafka;

import com.example.ecommerce.payment.domain.model.PaymentMethod;
import com.example.ecommerce.payment.domain.port.inbound.ProcessPaymentUseCase;
import com.example.ecommerce.shared.event.OrderCreatedIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventConsumer.class);

    private final ProcessPaymentUseCase processPayment;

    public OrderCreatedEventConsumer(ProcessPaymentUseCase processPayment) {
        this.processPayment = processPayment;
    }

    @KafkaListener(
            topics = OrderCreatedIntegrationEvent.TOPIC,
            groupId = "${spring.kafka.consumer.group-id}")
    public void on(OrderCreatedIntegrationEvent event) {
        log.info("OrderCreated received order={} amount={} {}",
                event.orderId(), event.totalAmount(), event.currency());

        // Idempotency key derived from order id so retries collapse to the same payment.
        String idempotencyKey = "order-" + event.orderId();
        processPayment.process(new ProcessPaymentUseCase.ProcessPaymentCommand(
                event.orderId(),
                event.buyerId(),
                idempotencyKey,
                PaymentMethod.CREDIT_CARD,
                event.totalAmount(),
                event.currency()));
    }
}
