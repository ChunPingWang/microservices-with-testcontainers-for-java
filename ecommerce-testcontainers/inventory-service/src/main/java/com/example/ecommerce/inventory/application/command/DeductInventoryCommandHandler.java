package com.example.ecommerce.inventory.application.command;

import com.example.ecommerce.inventory.domain.model.SkuId;
import com.example.ecommerce.inventory.domain.model.Stock;
import com.example.ecommerce.inventory.domain.port.inbound.DeductInventoryUseCase;
import com.example.ecommerce.inventory.domain.port.outbound.DistributedLockPort;
import com.example.ecommerce.inventory.domain.port.outbound.StockRepository;
import com.example.ecommerce.shared.domain.Quantity;
import com.example.ecommerce.shared.event.InventoryDeductedIntegrationEvent;
import com.example.ecommerce.shared.event.InventoryDeductionFailedIntegrationEvent;
import com.example.ecommerce.shared.port.EventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

public class DeductInventoryCommandHandler implements DeductInventoryUseCase {

    private static final Duration LOCK_WAIT = Duration.ofSeconds(5);
    private static final Duration LOCK_HOLD = Duration.ofSeconds(10);

    private final StockRepository stockRepository;
    private final DistributedLockPort lock;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public DeductInventoryCommandHandler(StockRepository stockRepository,
                                         DistributedLockPort lock,
                                         EventPublisher eventPublisher,
                                         Clock clock) {
        this.stockRepository = stockRepository;
        this.lock = lock;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    public void deduct(DeductInventoryCommand command) {
        for (DeductInventoryCommand.Line line : command.lines()) {
            try {
                deductLine(command.orderId(), line);
            } catch (Stock.InsufficientStockException ex) {
                eventPublisher.publish(new InventoryDeductionFailedIntegrationEvent(
                        UUID.randomUUID(), clock.instant(),
                        command.orderId(), ex.getMessage()));
                return; // first failure aborts the order
            }
        }
    }

    private void deductLine(UUID orderId, DeductInventoryCommand.Line line) {
        SkuId sku = new SkuId(line.sku());
        Quantity qty = Quantity.of(line.quantity());
        lock.withLock("stock:" + sku.value(), LOCK_WAIT, LOCK_HOLD, () -> {
            Stock stock = stockRepository.findBySku(sku)
                    .orElseGet(() -> Stock.seed(sku, 0));
            stock.reserve(orderId, qty);
            stock.commit(orderId);
            stockRepository.save(stock);

            eventPublisher.publish(new InventoryDeductedIntegrationEvent(
                    UUID.randomUUID(), clock.instant(),
                    orderId, sku.value(), qty.value()));
            return null;
        });
    }
}
