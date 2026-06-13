package com.example.ecommerce.inventory.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
class ReservationJpaEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected ReservationJpaEntity() {}

    ReservationJpaEntity(UUID orderId, int quantity) {
        this.orderId = orderId;
        this.quantity = quantity;
    }

    public UUID getOrderId() { return orderId; }
    public int getQuantity() { return quantity; }
}
