package com.example.ecommerce.product.domain.model;

import java.time.Instant;
import java.util.UUID;

public sealed interface OrderStatus
        permits OrderStatus.Created,
                OrderStatus.Paid,
                OrderStatus.Completed,
                OrderStatus.Cancelled {

    Instant at();

    record Created(Instant at) implements OrderStatus {}
    record Paid(Instant at, UUID paymentId) implements OrderStatus {}
    record Completed(Instant at) implements OrderStatus {}
    record Cancelled(Instant at, String reason) implements OrderStatus {}
}
