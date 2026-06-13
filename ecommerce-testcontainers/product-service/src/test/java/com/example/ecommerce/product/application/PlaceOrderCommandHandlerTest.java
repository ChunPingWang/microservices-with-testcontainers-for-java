package com.example.ecommerce.product.application;

import com.example.ecommerce.product.application.command.PlaceOrderCommandHandler;
import com.example.ecommerce.product.domain.model.OrderId;
import com.example.ecommerce.product.domain.port.inbound.PlaceOrderUseCase;
import com.example.ecommerce.product.fakes.InMemoryEventPublisher;
import com.example.ecommerce.product.fakes.InMemoryOrderWriteRepository;
import com.example.ecommerce.shared.event.IntegrationEvent;
import com.example.ecommerce.shared.event.OrderCreatedIntegrationEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderCommandHandlerTest {

    private final InMemoryOrderWriteRepository orderRepo = new InMemoryOrderWriteRepository();
    private final InMemoryEventPublisher events = new InMemoryEventPublisher();
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-13T00:00:00Z"), ZoneOffset.UTC);
    private final PlaceOrderCommandHandler handler = new PlaceOrderCommandHandler(orderRepo, events, clock);

    @Test
    void persists_order_and_publishes_integration_event() {
        OrderId id = handler.place(new PlaceOrderUseCase.PlaceOrderCommand(
                "buyer-1",
                "USD",
                List.of(
                        new PlaceOrderUseCase.PlaceOrderCommand.Line("SKU-A", 2, new BigDecimal("10.00")),
                        new PlaceOrderUseCase.PlaceOrderCommand.Line("SKU-B", 1, new BigDecimal("5.50"))
                )
        ));

        assertThat(orderRepo.findById(id)).isPresent();
        List<IntegrationEvent> published = events.published();
        assertThat(published).hasSize(1);
        assertThat(published.getFirst()).isInstanceOfSatisfying(
                OrderCreatedIntegrationEvent.class,
                e -> {
                    assertThat(e.orderId()).isEqualTo(id.value());
                    assertThat(e.totalAmount()).isEqualByComparingTo("25.50");
                    assertThat(e.currency()).isEqualTo("USD");
                    assertThat(e.lines()).hasSize(2);
                });
    }
}
