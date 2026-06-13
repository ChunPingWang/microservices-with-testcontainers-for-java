package com.example.ecommerce.product.application.command;

import com.example.ecommerce.product.domain.event.OrderEvent;
import com.example.ecommerce.product.domain.model.Order;
import com.example.ecommerce.product.domain.model.OrderId;
import com.example.ecommerce.product.domain.model.OrderLine;
import com.example.ecommerce.product.domain.port.inbound.PlaceOrderUseCase;
import com.example.ecommerce.product.domain.port.outbound.OrderWriteRepository;
import com.example.ecommerce.shared.domain.Money;
import com.example.ecommerce.shared.domain.Quantity;
import com.example.ecommerce.shared.event.OrderCreatedIntegrationEvent;
import com.example.ecommerce.shared.port.EventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public class PlaceOrderCommandHandler implements PlaceOrderUseCase {

    private final OrderWriteRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public PlaceOrderCommandHandler(OrderWriteRepository orderRepository,
                                    EventPublisher eventPublisher,
                                    Clock clock) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public OrderId place(PlaceOrderCommand command) {
        Currency currency = Currency.getInstance(command.currencyCode());
        List<OrderLine> lines = command.lines().stream()
                .map(l -> new OrderLine(
                        l.sku(),
                        Quantity.of(l.quantity()),
                        new Money(l.unitPrice(), currency)))
                .toList();

        Order order = Order.place(command.buyerId(), lines, clock);
        orderRepository.save(order);

        // Translate domain events → integration events for cross-service propagation.
        for (OrderEvent domainEvent : order.drainEvents()) {
            if (domainEvent instanceof OrderEvent.Created created) {
                publishCreated(created, lines);
            }
        }
        return order.id();
    }

    private void publishCreated(OrderEvent.Created created, List<OrderLine> lines) {
        Instant now = clock.instant();
        List<OrderCreatedIntegrationEvent.Line> integrationLines = lines.stream()
                .map(l -> new OrderCreatedIntegrationEvent.Line(
                        l.sku(), l.quantity().value(), l.unitPrice().amount()))
                .toList();
        eventPublisher.publish(new OrderCreatedIntegrationEvent(
                UUID.randomUUID(),
                now,
                created.orderId().value(),
                created.buyerId(),
                created.totalAmount().amount(),
                created.totalAmount().currency().getCurrencyCode(),
                integrationLines));
    }
}
