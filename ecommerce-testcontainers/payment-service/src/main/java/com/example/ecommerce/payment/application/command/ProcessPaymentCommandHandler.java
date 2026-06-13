package com.example.ecommerce.payment.application.command;

import com.example.ecommerce.payment.domain.model.IdempotencyKey;
import com.example.ecommerce.payment.domain.model.Payment;
import com.example.ecommerce.payment.domain.model.PaymentId;
import com.example.ecommerce.payment.domain.port.inbound.ProcessPaymentUseCase;
import com.example.ecommerce.payment.domain.port.outbound.NotificationPort;
import com.example.ecommerce.payment.domain.port.outbound.PaymentGateway;
import com.example.ecommerce.payment.domain.port.outbound.PaymentRepository;
import com.example.ecommerce.payment.domain.port.outbound.ReceiptStoragePort;
import com.example.ecommerce.payment.domain.service.PaymentValidationService;
import com.example.ecommerce.shared.domain.Money;
import com.example.ecommerce.shared.event.PaymentCompletedIntegrationEvent;
import com.example.ecommerce.shared.event.PaymentFailedIntegrationEvent;
import com.example.ecommerce.shared.port.EventPublisher;

import java.time.Clock;
import java.util.Currency;
import java.util.UUID;

/**
 * Orchestrates one payment attempt: idempotency check → validation →
 * gateway authorisation → receipt storage → notification → event publish.
 *
 * <p>This is the only place where all collaborators meet. The domain
 * aggregate stays pure; the application service is the glue.
 */
public class ProcessPaymentCommandHandler implements ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final ReceiptStoragePort receiptStorage;
    private final NotificationPort notification;
    private final EventPublisher eventPublisher;
    private final PaymentValidationService validation;
    private final Clock clock;

    public ProcessPaymentCommandHandler(
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            ReceiptStoragePort receiptStorage,
            NotificationPort notification,
            EventPublisher eventPublisher,
            PaymentValidationService validation,
            Clock clock) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.receiptStorage = receiptStorage;
        this.notification = notification;
        this.eventPublisher = eventPublisher;
        this.validation = validation;
        this.clock = clock;
    }

    @Override
    public PaymentId process(ProcessPaymentCommand command) {
        IdempotencyKey key = new IdempotencyKey(command.idempotencyKey());

        // Idempotent: return the existing payment id if we've seen this key before.
        var existing = paymentRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return existing.get().id();
        }

        Money amount = new Money(command.amount(), Currency.getInstance(command.currency()));
        Payment payment = Payment.initiate(
                command.orderId(), command.buyerId(), key, command.method(), amount, clock);
        validation.validate(payment);
        paymentRepository.save(payment);

        var auth = paymentGateway.authorise(command.method(), amount, key.value());
        if (!auth.success()) {
            payment.fail(auth.failureReason(), clock);
            paymentRepository.save(payment);
            notification.notifyFailed(payment.orderId(), payment.buyerId(), auth.failureReason());
            eventPublisher.publish(new PaymentFailedIntegrationEvent(
                    UUID.randomUUID(), clock.instant(),
                    payment.orderId(), auth.failureReason()));
            return payment.id();
        }
        payment.authorise(auth.authCode(), clock);

        byte[] receiptPdf = ("Receipt for payment " + payment.id().value()
                + "\nOrder: " + payment.orderId()
                + "\nAmount: " + amount.amount() + " " + amount.currency().getCurrencyCode()).getBytes();
        var receiptUri = receiptStorage.storeReceipt(payment.id().value().toString(), receiptPdf);

        payment.complete(receiptUri.toString(), clock);
        paymentRepository.save(payment);
        notification.notifyPaid(payment.orderId(), payment.buyerId(), receiptUri.toString());

        eventPublisher.publish(new PaymentCompletedIntegrationEvent(
                UUID.randomUUID(), clock.instant(),
                payment.id().value(), payment.orderId(),
                amount.amount(), amount.currency().getCurrencyCode(),
                receiptUri.toString()));
        return payment.id();
    }
}
