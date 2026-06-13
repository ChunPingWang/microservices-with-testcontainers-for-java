package com.example.ecommerce.product.adapter.outbound.persistence;

import com.example.ecommerce.product.domain.model.Order;
import com.example.ecommerce.product.domain.model.OrderId;
import com.example.ecommerce.product.domain.model.OrderLine;
import com.example.ecommerce.product.domain.model.OrderStatus;
import com.example.ecommerce.product.domain.port.outbound.OrderWriteRepository;
import com.example.ecommerce.shared.domain.Money;
import com.example.ecommerce.shared.domain.Quantity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

/**
 * Anti-corruption layer between the rich {@link Order} aggregate and the
 * JPA persistence model. Domain classes never travel through the JPA
 * entity — every read/write goes through explicit mappers.
 */
@Repository
@Transactional
public class JpaOrderWriteRepository implements OrderWriteRepository {

    private final OrderJpaRepository jpa;

    public JpaOrderWriteRepository(OrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = jpa.findById(order.id().value()).orElseGet(OrderJpaEntity::new);
        entity.setId(order.id().value());
        entity.setBuyerId(order.buyerId());
        entity.setTotalAmount(order.totalAmount().amount());
        entity.setTotalCurrency(order.totalAmount().currency().getCurrencyCode());
        applyStatus(entity, order.status());
        entity.setLines(toJpaLines(order.lines()));
        // Only set version on update — Hibernate auto-assigns for insert.
        if (order.version() > 0) {
            entity.setVersion(order.version());
        }
        OrderJpaEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(OrderId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    private void applyStatus(OrderJpaEntity entity, OrderStatus status) {
        OffsetDateTime at = OffsetDateTime.ofInstant(status.at(), ZoneOffset.UTC);
        entity.setStatusAt(at);
        switch (status) {
            case OrderStatus.Created __ -> {
                entity.setStatus("CREATED");
                entity.setPaymentId(null);
                entity.setCancelReason(null);
            }
            case OrderStatus.Paid paid -> {
                entity.setStatus("PAID");
                entity.setPaymentId(paid.paymentId());
            }
            case OrderStatus.Completed __ -> entity.setStatus("COMPLETED");
            case OrderStatus.Cancelled cancelled -> {
                entity.setStatus("CANCELLED");
                entity.setCancelReason(cancelled.reason());
            }
        }
    }

    private List<OrderLineJpaEntity> toJpaLines(List<OrderLine> lines) {
        List<OrderLineJpaEntity> mapped = new ArrayList<>(lines.size());
        for (OrderLine l : lines) {
            mapped.add(new OrderLineJpaEntity(
                    l.sku(),
                    l.quantity().value(),
                    l.unitPrice().amount(),
                    l.unitPrice().currency().getCurrencyCode()));
        }
        return mapped;
    }

    private Order toDomain(OrderJpaEntity e) {
        Currency totalCurrency = Currency.getInstance(e.getTotalCurrency());
        Money total = new Money(e.getTotalAmount(), totalCurrency);
        List<OrderLine> lines = new ArrayList<>(e.getLines().size());
        for (OrderLineJpaEntity le : e.getLines()) {
            lines.add(new OrderLine(
                    le.getSku(),
                    Quantity.of(le.getQuantity()),
                    new Money(le.getUnitPrice(), Currency.getInstance(le.getUnitCurrency()))));
        }
        OrderStatus status = toDomainStatus(e);
        return Order.rehydrate(new OrderId(e.getId()), e.getBuyerId(), lines, total, status, e.getVersion());
    }

    private OrderStatus toDomainStatus(OrderJpaEntity e) {
        var at = e.getStatusAt().toInstant();
        return switch (e.getStatus()) {
            case "CREATED" -> new OrderStatus.Created(at);
            case "PAID" -> new OrderStatus.Paid(at, e.getPaymentId());
            case "COMPLETED" -> new OrderStatus.Completed(at);
            case "CANCELLED" -> new OrderStatus.Cancelled(at, e.getCancelReason());
            default -> throw new IllegalStateException("unknown status: " + e.getStatus());
        };
    }

}
