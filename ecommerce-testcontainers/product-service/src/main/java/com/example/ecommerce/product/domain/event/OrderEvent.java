package com.example.ecommerce.product.domain.event;

import com.example.ecommerce.product.domain.model.OrderId;
import com.example.ecommerce.shared.domain.Money;

import java.time.Instant;
import java.util.UUID;

public sealed interface OrderEvent
        permits OrderEvent.Created,
                OrderEvent.Paid,
                OrderEvent.Cancelled {

    OrderId orderId();
    Instant occurredAt();

    record Created(OrderId orderId, Instant occurredAt, String buyerId, Money totalAmount) implements OrderEvent {}
    record Paid(OrderId orderId, Instant occurredAt, UUID paymentId) implements OrderEvent {}
    record Cancelled(OrderId orderId, Instant occurredAt, String reason) implements OrderEvent {}
}
