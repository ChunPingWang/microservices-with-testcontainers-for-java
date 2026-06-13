package com.example.ecommerce.inventory.domain.model;

import com.example.ecommerce.shared.domain.Quantity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Stock aggregate: holds available + reserved quantities for a single SKU,
 * with an optimistic-lock version field so concurrent deductions are detected
 * at the database layer.
 *
 * <p>Reservations live on the aggregate so that compensation (cancelOrder)
 * can release them precisely.
 */
public final class Stock {

    private final SkuId sku;
    private Quantity available;
    private final Map<UUID, Quantity> reservations;
    private long version;

    private Stock(SkuId sku, Quantity available, Map<UUID, Quantity> reservations, long version) {
        this.sku = Objects.requireNonNull(sku);
        this.available = Objects.requireNonNull(available);
        this.reservations = new HashMap<>(reservations);
        this.version = version;
    }

    public static Stock seed(SkuId sku, int initial) {
        return new Stock(sku, Quantity.of(initial), Map.of(), 0L);
    }

    public static Stock rehydrate(SkuId sku, Quantity available, Map<UUID, Quantity> reservations, long version) {
        return new Stock(sku, available, reservations, version);
    }

    /**
     * Reserve {@code amount} for {@code orderId}. Idempotent: replaying with
     * the same orderId is a no-op.
     */
    public void reserve(UUID orderId, Quantity amount) {
        if (reservations.containsKey(orderId)) return;
        if (available.lessThan(amount)) {
            throw new InsufficientStockException(sku, available, amount);
        }
        available = available.subtract(amount);
        reservations.put(orderId, amount);
    }

    /** Commit a previously-made reservation, removing it from the books. */
    public void commit(UUID orderId) {
        Quantity reserved = reservations.remove(orderId);
        if (reserved == null) {
            throw new IllegalStateException("no reservation for order " + orderId);
        }
        // Nothing else to do: available was already reduced at reserve time.
    }

    /** Release a reservation back to available (compensation). */
    public void release(UUID orderId) {
        Quantity reserved = reservations.remove(orderId);
        if (reserved == null) return; // idempotent
        available = available.add(reserved);
    }

    public StockLevel level() {
        Quantity totalReserved = reservations.values().stream()
                .reduce(Quantity.zero(), Quantity::add);
        return new StockLevel(available, totalReserved);
    }

    public List<Reservation> reservations() {
        List<Reservation> out = new ArrayList<>(reservations.size());
        reservations.forEach((id, q) -> out.add(new Reservation(id, q)));
        return out;
    }

    public SkuId sku() { return sku; }
    public Quantity available() { return available; }
    public long version() { return version; }

    public static final class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(SkuId sku, Quantity available, Quantity requested) {
            super("SKU " + sku.value() + " has " + available.value() + " available, requested " + requested.value());
        }
    }
}
