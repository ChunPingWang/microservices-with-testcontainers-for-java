package com.example.ecommerce.product.domain.port.inbound;

import com.example.ecommerce.product.domain.model.OrderId;

import java.math.BigDecimal;
import java.util.List;

public interface PlaceOrderUseCase {

    OrderId place(PlaceOrderCommand command);

    record PlaceOrderCommand(String buyerId, String currencyCode, List<Line> lines) {
        public record Line(String sku, int quantity, BigDecimal unitPrice) {}
    }
}
