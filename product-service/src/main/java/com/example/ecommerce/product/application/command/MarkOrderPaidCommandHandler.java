package com.example.ecommerce.product.application.command;

import com.example.ecommerce.product.domain.model.Order;
import com.example.ecommerce.product.domain.model.OrderId;
import com.example.ecommerce.product.domain.port.inbound.CompleteOrderUseCase;
import com.example.ecommerce.product.domain.port.inbound.MarkOrderPaidUseCase;
import com.example.ecommerce.product.domain.port.outbound.OrderWriteRepository;

import java.time.Clock;
import java.util.UUID;

public class MarkOrderPaidCommandHandler implements MarkOrderPaidUseCase, CompleteOrderUseCase {

    private final OrderWriteRepository orderRepository;
    private final Clock clock;

    public MarkOrderPaidCommandHandler(OrderWriteRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    @Override
    public void markPaid(OrderId orderId, UUID paymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderId));
        order.markPaid(paymentId, clock);
        orderRepository.save(order);
    }

    @Override
    public void complete(OrderId orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderId));
        order.complete(clock);
        orderRepository.save(order);
    }
}
