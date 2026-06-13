package com.example.ecommerce.product.domain.port.inbound;

import com.example.ecommerce.product.domain.model.OrderId;

public interface CompleteOrderUseCase {
    void complete(OrderId orderId);
}
