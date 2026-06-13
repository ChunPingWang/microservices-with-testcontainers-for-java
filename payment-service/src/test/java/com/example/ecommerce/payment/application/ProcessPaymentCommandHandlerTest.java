package com.example.ecommerce.payment.application;

import com.example.ecommerce.payment.application.command.ProcessPaymentCommandHandler;
import com.example.ecommerce.payment.domain.model.PaymentId;
import com.example.ecommerce.payment.domain.model.PaymentMethod;
import com.example.ecommerce.payment.domain.model.PaymentStatus;
import com.example.ecommerce.payment.domain.port.inbound.ProcessPaymentUseCase;
import com.example.ecommerce.payment.domain.service.PaymentValidationService;
import com.example.ecommerce.payment.fakes.InMemoryEventPublisher;
import com.example.ecommerce.payment.fakes.InMemoryPaymentRepository;
import com.example.ecommerce.payment.fakes.InMemoryReceiptStorage;
import com.example.ecommerce.payment.fakes.RecordingNotification;
import com.example.ecommerce.payment.fakes.StubPaymentGateway;
import com.example.ecommerce.shared.event.PaymentCompletedIntegrationEvent;
import com.example.ecommerce.shared.event.PaymentFailedIntegrationEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessPaymentCommandHandlerTest {

    private final InMemoryPaymentRepository repo = new InMemoryPaymentRepository();
    private final StubPaymentGateway gateway = new StubPaymentGateway(new BigDecimal("100.00"));
    private final InMemoryReceiptStorage storage = new InMemoryReceiptStorage();
    private final RecordingNotification notification = new RecordingNotification();
    private final InMemoryEventPublisher events = new InMemoryEventPublisher();
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-13T00:00:00Z"), ZoneOffset.UTC);

    private final ProcessPaymentCommandHandler handler = new ProcessPaymentCommandHandler(
            repo, gateway, storage, notification, events, new PaymentValidationService(), clock);

    @Test
    void successful_payment_completes_and_publishes_event() {
        UUID orderId = UUID.randomUUID();
        PaymentId id = handler.process(new ProcessPaymentUseCase.ProcessPaymentCommand(
                orderId, "buyer-1", "key-1", PaymentMethod.CREDIT_CARD,
                new BigDecimal("25.00"), "USD"));

        var payment = repo.findById(id).orElseThrow();
        assertThat(payment.status()).isInstanceOf(PaymentStatus.Completed.class);

        assertThat(notification.paidEvents()).hasSize(1);
        assertThat(events.published()).hasSize(1).first()
                .isInstanceOfSatisfying(PaymentCompletedIntegrationEvent.class,
                        e -> assertThat(e.orderId()).isEqualTo(orderId));
    }

    @Test
    void failed_authorisation_publishes_failed_event() {
        UUID orderId = UUID.randomUUID();
        handler.process(new ProcessPaymentUseCase.ProcessPaymentCommand(
                orderId, "buyer-1", "key-fail", PaymentMethod.CREDIT_CARD,
                new BigDecimal("999.00"), "USD"));

        assertThat(notification.failedEvents()).hasSize(1);
        assertThat(events.published()).hasSize(1).first()
                .isInstanceOfSatisfying(PaymentFailedIntegrationEvent.class,
                        e -> assertThat(e.orderId()).isEqualTo(orderId));
    }

    @Test
    void idempotent_replay_returns_same_payment_id_no_new_event() {
        UUID orderId = UUID.randomUUID();
        PaymentId first = handler.process(new ProcessPaymentUseCase.ProcessPaymentCommand(
                orderId, "buyer-1", "key-idem", PaymentMethod.CREDIT_CARD,
                new BigDecimal("25.00"), "USD"));

        int eventsBefore = events.published().size();
        PaymentId second = handler.process(new ProcessPaymentUseCase.ProcessPaymentCommand(
                orderId, "buyer-1", "key-idem", PaymentMethod.CREDIT_CARD,
                new BigDecimal("25.00"), "USD"));

        assertThat(second).isEqualTo(first);
        assertThat(events.published()).hasSize(eventsBefore); // no second publish
    }
}
