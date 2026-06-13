package com.example.ecommerce.product.contract;

import com.example.ecommerce.product.domain.model.Order;
import com.example.ecommerce.product.domain.model.OrderLine;
import com.example.ecommerce.product.domain.model.OrderStatus;
import com.example.ecommerce.product.domain.port.outbound.OrderWriteRepository;
import com.example.ecommerce.shared.domain.Money;
import com.example.ecommerce.shared.domain.Quantity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioural contract that every implementation of {@link OrderWriteRepository}
 * must satisfy. Concrete implementations subclass this and provide the actual
 * adapter from {@link #repository()}; the test suite then runs every method.
 *
 * <p>This is how we guarantee {@code InMemoryOrderWriteRepository} and
 * {@code JpaOrderWriteRepository} are truly substitutable (Liskov).
 */
public abstract class OrderWriteRepositoryContract {

    private static final Clock FIXED = Clock.fixed(
            Instant.parse("2026-06-13T00:00:00Z"), ZoneOffset.UTC);

    protected abstract OrderWriteRepository repository();

    @Test
    void save_then_load_preserves_state() {
        Order order = Order.place("buyer-1", List.of(
                new OrderLine("SKU-A", Quantity.of(2), Money.of("10.00", "USD"))
        ), FIXED);
        repository().save(order);

        Order loaded = repository().findById(order.id()).orElseThrow();
        assertThat(loaded.buyerId()).isEqualTo("buyer-1");
        assertThat(loaded.totalAmount().amount()).isEqualByComparingTo("20.00");
        assertThat(loaded.lines()).hasSize(1);
        assertThat(loaded.status()).isInstanceOf(OrderStatus.Created.class);
    }

    @Test
    void status_transitions_round_trip() {
        Order order = Order.place("buyer-2", List.of(
                new OrderLine("SKU-A", Quantity.of(1), Money.of("10.00", "USD"))
        ), FIXED);
        order = repository().save(order);
        UUID paymentId = UUID.randomUUID();
        order.markPaid(paymentId, FIXED);
        repository().save(order);

        Order loaded = repository().findById(order.id()).orElseThrow();
        assertThat(loaded.status()).isInstanceOfSatisfying(
                OrderStatus.Paid.class,
                s -> assertThat(s.paymentId()).isEqualTo(paymentId));
    }

    @Test
    void cancelled_status_keeps_reason() {
        Order order = Order.place("buyer-3", List.of(
                new OrderLine("SKU-A", Quantity.of(1), Money.of("10.00", "USD"))
        ), FIXED);
        order = repository().save(order);
        order.cancel("user remorse", FIXED);
        repository().save(order);

        Order loaded = repository().findById(order.id()).orElseThrow();
        assertThat(loaded.status()).isInstanceOfSatisfying(
                OrderStatus.Cancelled.class,
                s -> assertThat(s.reason()).isEqualTo("user remorse"));
    }
}
