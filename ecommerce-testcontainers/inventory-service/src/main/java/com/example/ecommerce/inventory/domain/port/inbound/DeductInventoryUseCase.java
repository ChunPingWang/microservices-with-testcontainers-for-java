package com.example.ecommerce.inventory.domain.port.inbound;

import java.util.List;
import java.util.UUID;

public interface DeductInventoryUseCase {

    void deduct(DeductInventoryCommand command);

    record DeductInventoryCommand(UUID orderId, List<Line> lines) {
        public record Line(String sku, int quantity) {}
    }
}
