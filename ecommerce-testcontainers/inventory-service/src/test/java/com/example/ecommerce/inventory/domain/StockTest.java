package com.example.ecommerce.inventory.domain;

import com.example.ecommerce.inventory.domain.model.SkuId;
import com.example.ecommerce.inventory.domain.model.Stock;
import com.example.ecommerce.shared.domain.Quantity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    @Test
    void reserve_reduces_available_and_records_reservation() {
        Stock stock = Stock.seed(new SkuId("SKU-A"), 10);
        UUID orderId = UUID.randomUUID();
        stock.reserve(orderId, Quantity.of(3));

        assertThat(stock.available().value()).isEqualTo(7);
        assertThat(stock.level().reserved().value()).isEqualTo(3);
    }

    @Test
    void reserve_is_idempotent_per_order() {
        Stock stock = Stock.seed(new SkuId("SKU-A"), 10);
        UUID orderId = UUID.randomUUID();
        stock.reserve(orderId, Quantity.of(3));
        stock.reserve(orderId, Quantity.of(3)); // no-op
        assertThat(stock.available().value()).isEqualTo(7);
    }

    @Test
    void reserve_throws_when_insufficient() {
        Stock stock = Stock.seed(new SkuId("SKU-A"), 2);
        assertThatThrownBy(() -> stock.reserve(UUID.randomUUID(), Quantity.of(5)))
                .isInstanceOf(Stock.InsufficientStockException.class);
    }

    @Test
    void commit_removes_reservation_keeping_available_unchanged() {
        Stock stock = Stock.seed(new SkuId("SKU-A"), 10);
        UUID orderId = UUID.randomUUID();
        stock.reserve(orderId, Quantity.of(3));
        stock.commit(orderId);

        assertThat(stock.available().value()).isEqualTo(7); // already subtracted at reserve
        assertThat(stock.level().reserved().value()).isEqualTo(0);
    }

    @Test
    void release_returns_quantity_to_available() {
        Stock stock = Stock.seed(new SkuId("SKU-A"), 10);
        UUID orderId = UUID.randomUUID();
        stock.reserve(orderId, Quantity.of(4));
        stock.release(orderId);
        assertThat(stock.available().value()).isEqualTo(10);
    }
}
