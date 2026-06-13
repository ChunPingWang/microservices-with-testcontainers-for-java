package com.example.ecommerce.product.adapter.inbound.rest;

import com.example.ecommerce.product.domain.port.inbound.PlaceOrderUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final PlaceOrderUseCase placeOrder;

    public OrderController(PlaceOrderUseCase placeOrder) {
        this.placeOrder = placeOrder;
    }

    @PostMapping
    public ResponseEntity<PlaceOrderResponse> place(@Valid @RequestBody PlaceOrderRequest request) {
        List<PlaceOrderUseCase.PlaceOrderCommand.Line> lines = request.lines().stream()
                .map(l -> new PlaceOrderUseCase.PlaceOrderCommand.Line(l.sku(), l.quantity(), l.unitPrice()))
                .toList();
        UUID id = placeOrder.place(new PlaceOrderUseCase.PlaceOrderCommand(
                request.buyerId(), request.currency(), lines)).value();
        return ResponseEntity.created(URI.create("/api/orders/" + id))
                .body(new PlaceOrderResponse(id));
    }

    public record PlaceOrderRequest(
            @NotNull @Size(min = 1) String buyerId,
            @NotNull @Size(min = 3, max = 3) String currency,
            @NotEmpty List<@Valid LineRequest> lines) {

        public record LineRequest(
                @NotNull @Size(min = 1) String sku,
                @Positive int quantity,
                @NotNull BigDecimal unitPrice) {}
    }

    public record PlaceOrderResponse(UUID orderId) {}
}
