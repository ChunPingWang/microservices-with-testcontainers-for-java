package com.example.ecommerce.inventory.application;

import com.example.ecommerce.inventory.application.command.DeductInventoryCommandHandler;
import com.example.ecommerce.inventory.domain.model.SkuId;
import com.example.ecommerce.inventory.domain.port.inbound.DeductInventoryUseCase;
import com.example.ecommerce.inventory.fakes.InMemoryEventPublisher;
import com.example.ecommerce.inventory.fakes.InMemoryStockRepository;
import com.example.ecommerce.inventory.fakes.ReentrantLockAdapter;
import com.example.ecommerce.shared.event.InventoryDeductedIntegrationEvent;
import com.example.ecommerce.shared.event.InventoryDeductionFailedIntegrationEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeductInventoryCommandHandlerTest {

    private final InMemoryStockRepository repo = new InMemoryStockRepository();
    private final ReentrantLockAdapter lock = new ReentrantLockAdapter();
    private final InMemoryEventPublisher events = new InMemoryEventPublisher();
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-13T00:00:00Z"), ZoneOffset.UTC);
    private final DeductInventoryCommandHandler handler =
            new DeductInventoryCommandHandler(repo, lock, events, clock);

    @Test
    void deduct_publishes_one_event_per_line_when_stock_available() {
        repo.seed(new SkuId("SKU-A"), 10);
        repo.seed(new SkuId("SKU-B"), 5);

        handler.deduct(new DeductInventoryUseCase.DeductInventoryCommand(
                UUID.randomUUID(),
                List.of(
                        new DeductInventoryUseCase.DeductInventoryCommand.Line("SKU-A", 2),
                        new DeductInventoryUseCase.DeductInventoryCommand.Line("SKU-B", 1))));

        assertThat(events.published()).hasSize(2);
        assertThat(events.published())
                .allSatisfy(e -> assertThat(e).isInstanceOf(InventoryDeductedIntegrationEvent.class));
    }

    @Test
    void deduct_emits_failed_event_when_insufficient() {
        repo.seed(new SkuId("SKU-A"), 2);
        handler.deduct(new DeductInventoryUseCase.DeductInventoryCommand(
                UUID.randomUUID(),
                List.of(new DeductInventoryUseCase.DeductInventoryCommand.Line("SKU-A", 5))));

        assertThat(events.published()).hasSize(1).first()
                .isInstanceOf(InventoryDeductionFailedIntegrationEvent.class);
    }
}
