package com.example.ecommerce.product.domain.model;

import com.example.ecommerce.product.domain.event.OrderEvent;
import com.example.ecommerce.shared.domain.Money;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Order aggregate root. State changes go through behaviour methods that
 * also append a {@link OrderEvent} so the application layer can publish them.
 */
public final class Order {

    private final OrderId id;
    private final String buyerId;
    private final List<OrderLine> lines;
    private final Money totalAmount;
    private OrderStatus status;
    private long version; // for JPA optimistic locking
    private final List<OrderEvent> uncommittedEvents = new ArrayList<>();

    private Order(OrderId id, String buyerId, List<OrderLine> lines, Money totalAmount, OrderStatus status, long version) {
        this.id = Objects.requireNonNull(id);
        this.buyerId = Objects.requireNonNull(buyerId);
        this.lines = List.copyOf(lines);
        this.totalAmount = Objects.requireNonNull(totalAmount);
        this.status = Objects.requireNonNull(status);
        this.version = version;
    }

    /** Factory for placing a new order. Calculates total and emits Created event. */
    public static Order place(String buyerId, List<OrderLine> lines, Clock clock) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("order must have at least one line");
        }
        Currency currency = lines.getFirst().unitPrice().currency();
        Money total = lines.stream()
                .map(OrderLine::subtotal)
                .reduce(Money.zero(currency), Money::add);
        OrderId id = OrderId.newId();
        Instant now = clock.instant();
        Order order = new Order(id, buyerId, lines, total, new OrderStatus.Created(now), 0L);
        order.uncommittedEvents.add(new OrderEvent.Created(id, now, buyerId, total));
        return order;
    }

    /** Rehydrate from persistence. Does not emit events. */
    public static Order rehydrate(OrderId id, String buyerId, List<OrderLine> lines,
                                  Money totalAmount, OrderStatus status, long version) {
        return new Order(id, buyerId, lines, totalAmount, status, version);
    }

    public void markPaid(UUID paymentId, Clock clock) {
        if (!(status instanceof OrderStatus.Created)) {
            throw new IllegalStateException("only Created orders can be paid; current=" + status);
        }
        Instant now = clock.instant();
        this.status = new OrderStatus.Paid(now, paymentId);
        this.uncommittedEvents.add(new OrderEvent.Paid(id, now, paymentId));
    }

    public void complete(Clock clock) {
        if (!(status instanceof OrderStatus.Paid)) {
            throw new IllegalStateException("only Paid orders can be completed; current=" + status);
        }
        this.status = new OrderStatus.Completed(clock.instant());
    }

    public void cancel(String reason, Clock clock) {
        if (status instanceof OrderStatus.Completed) {
            throw new IllegalStateException("completed orders cannot be cancelled");
        }
        Instant now = clock.instant();
        this.status = new OrderStatus.Cancelled(now, reason);
        this.uncommittedEvents.add(new OrderEvent.Cancelled(id, now, reason));
    }

    public OrderId id() { return id; }
    public String buyerId() { return buyerId; }
    public List<OrderLine> lines() { return lines; }
    public Money totalAmount() { return totalAmount; }
    public OrderStatus status() { return status; }
    public long version() { return version; }

    public List<OrderEvent> drainEvents() {
        List<OrderEvent> drained = List.copyOf(uncommittedEvents);
        uncommittedEvents.clear();
        return drained;
    }
}
