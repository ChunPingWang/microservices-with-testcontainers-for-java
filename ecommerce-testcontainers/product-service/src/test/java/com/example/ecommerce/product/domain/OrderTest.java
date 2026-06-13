package com.example.ecommerce.product.domain;

import com.example.ecommerce.product.domain.event.OrderEvent;
import com.example.ecommerce.product.domain.model.Order;
import com.example.ecommerce.product.domain.model.OrderLine;
import com.example.ecommerce.product.domain.model.OrderStatus;
import com.example.ecommerce.shared.domain.Money;
import com.example.ecommerce.shared.domain.Quantity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-06-13T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void place_calculates_total_and_emits_created_event() {
        Order order = Order.place("buyer-1", List.of(
                new OrderLine("SKU-A", Quantity.of(2), Money.of("10.00", "USD")),
                new OrderLine("SKU-B", Quantity.of(1), Money.of("5.50", "USD"))
        ), FIXED);

        assertThat(order.totalAmount().amount()).isEqualByComparingTo("25.50");
        assertThat(order.status()).isInstanceOf(OrderStatus.Created.class);
        List<OrderEvent> events = order.drainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).isInstanceOf(OrderEvent.Created.class);
        assertThat(order.drainEvents()).isEmpty(); // drained
    }

    @Test
    void place_empty_throws() {
        assertThatThrownBy(() -> Order.place("buyer", List.of(), FIXED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mark_paid_only_from_created_state() {
        Order order = Order.place("buyer", List.of(
                new OrderLine("SKU-A", Quantity.of(1), Money.of("10.00", "USD"))
        ), FIXED);
        order.drainEvents();

        UUID paymentId = UUID.randomUUID();
        order.markPaid(paymentId, FIXED);
        assertThat(order.status()).isInstanceOf(OrderStatus.Paid.class);

        // Cannot pay again
        assertThatThrownBy(() -> order.markPaid(UUID.randomUUID(), FIXED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void complete_only_from_paid_state() {
        Order order = Order.place("buyer", List.of(
                new OrderLine("SKU-A", Quantity.of(1), Money.of("10.00", "USD"))
        ), FIXED);
        assertThatThrownBy(() -> order.complete(FIXED))
                .isInstanceOf(IllegalStateException.class);

        order.markPaid(UUID.randomUUID(), FIXED);
        order.complete(FIXED);
        assertThat(order.status()).isInstanceOf(OrderStatus.Completed.class);
    }

    @Test
    void cancel_emits_cancelled_event_and_changes_status() {
        Order order = Order.place("buyer", List.of(
                new OrderLine("SKU-A", Quantity.of(1), Money.of("10.00", "USD"))
        ), FIXED);
        order.drainEvents();
        order.cancel("buyer changed mind", FIXED);

        assertThat(order.status()).isInstanceOf(OrderStatus.Cancelled.class);
        assertThat(order.drainEvents()).anyMatch(e -> e instanceof OrderEvent.Cancelled);
    }

    @Test
    void cannot_cancel_completed_order() {
        Order order = Order.place("buyer", List.of(
                new OrderLine("SKU-A", Quantity.of(1), Money.of("10.00", "USD"))
        ), FIXED);
        order.markPaid(UUID.randomUUID(), FIXED);
        order.complete(FIXED);

        assertThatThrownBy(() -> order.cancel("nope", FIXED))
                .isInstanceOf(IllegalStateException.class);
    }
}
