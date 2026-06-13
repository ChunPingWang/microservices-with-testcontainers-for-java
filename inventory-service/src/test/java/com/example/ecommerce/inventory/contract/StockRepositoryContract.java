package com.example.ecommerce.inventory.contract;

import com.example.ecommerce.inventory.domain.model.SkuId;
import com.example.ecommerce.inventory.domain.model.Stock;
import com.example.ecommerce.inventory.domain.port.outbound.StockRepository;
import com.example.ecommerce.shared.domain.Quantity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioural contract for {@link StockRepository}. Anything claiming to be
 * a StockRepository must round-trip stock state including reservations,
 * because that's what the application layer assumes when it reads/writes.
 */
public abstract class StockRepositoryContract {

    protected abstract StockRepository repository();

    /** Subclasses may reset state between tests if their backing store persists. */
    protected void reset() {}

    @Test
    void save_then_load_empty_stock() {
        reset();
        Stock stock = Stock.seed(new SkuId("CONTRACT-A"), 10);
        repository().save(stock);

        Stock loaded = repository().findBySku(new SkuId("CONTRACT-A")).orElseThrow();
        assertThat(loaded.available().value()).isEqualTo(10);
        assertThat(loaded.level().reserved().value()).isZero();
    }

    @Test
    void save_then_load_with_outstanding_reservation() {
        reset();
        Stock stock = Stock.seed(new SkuId("CONTRACT-B"), 10);
        UUID orderId = UUID.randomUUID();
        stock.reserve(orderId, Quantity.of(4));
        repository().save(stock);

        Stock loaded = repository().findBySku(new SkuId("CONTRACT-B")).orElseThrow();
        assertThat(loaded.available().value()).isEqualTo(6);
        assertThat(loaded.level().reserved().value()).isEqualTo(4);
        assertThat(loaded.reservations()).hasSize(1)
                .first().extracting(r -> r.orderId()).isEqualTo(orderId);
    }

    @Test
    void update_after_commit_clears_reservation() {
        reset();
        Stock stock = Stock.seed(new SkuId("CONTRACT-C"), 5);
        UUID orderId = UUID.randomUUID();
        stock.reserve(orderId, Quantity.of(2));
        stock = repository().save(stock);
        stock.commit(orderId);
        repository().save(stock);

        Stock loaded = repository().findBySku(new SkuId("CONTRACT-C")).orElseThrow();
        assertThat(loaded.available().value()).isEqualTo(3);
        assertThat(loaded.level().reserved().value()).isZero();
    }

    @Test
    void release_returns_quantity_to_available() {
        reset();
        Stock stock = Stock.seed(new SkuId("CONTRACT-D"), 10);
        UUID orderId = UUID.randomUUID();
        stock.reserve(orderId, Quantity.of(3));
        stock = repository().save(stock);
        stock.release(orderId);
        repository().save(stock);

        Stock loaded = repository().findBySku(new SkuId("CONTRACT-D")).orElseThrow();
        assertThat(loaded.available().value()).isEqualTo(10);
        assertThat(loaded.level().reserved().value()).isZero();
    }

    @Test
    void find_unknown_sku_is_empty() {
        reset();
        assertThat(repository().findBySku(new SkuId("NEVER-CREATED"))).isEmpty();
    }
}
