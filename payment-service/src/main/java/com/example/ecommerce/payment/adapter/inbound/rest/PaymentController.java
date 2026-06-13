package com.example.ecommerce.payment.adapter.inbound.rest;

import com.example.ecommerce.payment.domain.model.PaymentMethod;
import com.example.ecommerce.payment.domain.port.inbound.ProcessPaymentUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final ProcessPaymentUseCase processPayment;

    public PaymentController(ProcessPaymentUseCase processPayment) {
        this.processPayment = processPayment;
    }

    @PostMapping
    public ResponseEntity<ProcessResponse> process(@Valid @RequestBody ProcessRequest request) {
        UUID id = processPayment.process(new ProcessPaymentUseCase.ProcessPaymentCommand(
                request.orderId(),
                request.buyerId(),
                request.idempotencyKey(),
                request.method(),
                request.amount(),
                request.currency())).value();
        return ResponseEntity.created(URI.create("/api/payments/" + id))
                .body(new ProcessResponse(id));
    }

    public record ProcessRequest(
            @NotNull UUID orderId,
            @NotNull @Size(min = 1) String buyerId,
            @NotNull @Size(min = 1, max = 128) String idempotencyKey,
            @NotNull PaymentMethod method,
            @NotNull @Positive BigDecimal amount,
            @NotNull @Size(min = 3, max = 3) String currency) {}

    public record ProcessResponse(UUID paymentId) {}
}
