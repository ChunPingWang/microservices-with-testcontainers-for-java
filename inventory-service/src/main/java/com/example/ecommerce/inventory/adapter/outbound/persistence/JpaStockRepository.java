package com.example.ecommerce.inventory.adapter.outbound.persistence;

import com.example.ecommerce.inventory.domain.model.SkuId;
import com.example.ecommerce.inventory.domain.model.Stock;
import com.example.ecommerce.inventory.domain.port.outbound.StockRepository;
import com.example.ecommerce.shared.domain.Quantity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class JpaStockRepository implements StockRepository {

    private final StockJpaRepository jpa;

    public JpaStockRepository(StockJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Stock> findBySku(SkuId sku) {
        return jpa.findById(sku.value()).map(this::toDomain);
    }

    @Override
    public Stock save(Stock stock) {
        StockJpaEntity entity = jpa.findById(stock.sku().value()).orElseGet(StockJpaEntity::new);
        entity.setSku(stock.sku().value());
        entity.setAvailable(stock.available().value());
        if (stock.version() > 0) entity.setVersion(stock.version());

        entity.getReservations().clear();
        for (var r : stock.reservations()) {
            entity.getReservations().add(new ReservationJpaEntity(r.orderId(), r.quantity().value()));
        }
        return toDomain(jpa.save(entity));
    }

    private Stock toDomain(StockJpaEntity e) {
        Map<UUID, Quantity> reservations = new HashMap<>(e.getReservations().size());
        for (var r : e.getReservations()) {
            reservations.put(r.getOrderId(), Quantity.of(r.getQuantity()));
        }
        return Stock.rehydrate(
                new SkuId(e.getSku()),
                Quantity.of(e.getAvailable()),
                reservations,
                e.getVersion());
    }
}
