package com.example.ecommerce.inventory.adapter.inbound.rest;

import com.example.ecommerce.inventory.domain.port.inbound.DeductInventoryUseCase;
import com.example.ecommerce.inventory.domain.port.inbound.QueryStockUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final DeductInventoryUseCase deduct;
    private final QueryStockUseCase query;

    public InventoryController(DeductInventoryUseCase deduct, QueryStockUseCase query) {
        this.deduct = deduct;
        this.query = query;
    }

    @GetMapping("/stock/{sku}")
    public QueryStockUseCase.StockView stock(@PathVariable String sku) {
        return query.get(sku);
    }

    @PostMapping("/deductions")
    public ResponseEntity<Void> deductInventory(@Valid @RequestBody DeductRequest request) {
        deduct.deduct(new DeductInventoryUseCase.DeductInventoryCommand(
                request.orderId(),
                request.lines().stream()
                        .map(l -> new DeductInventoryUseCase.DeductInventoryCommand.Line(l.sku(), l.quantity()))
                        .toList()));
        return ResponseEntity.accepted().build();
    }

    public record DeductRequest(
            @NotNull UUID orderId,
            @NotEmpty List<@Valid Line> lines) {

        public record Line(
                @NotNull @Size(min = 1, max = 64) String sku,
                @Positive int quantity) {}
    }
}
