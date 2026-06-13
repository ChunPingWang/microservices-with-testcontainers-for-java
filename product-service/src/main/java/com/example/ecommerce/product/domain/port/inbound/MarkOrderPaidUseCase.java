package com.example.ecommerce.product.domain.port.inbound;

import com.example.ecommerce.product.domain.model.OrderId;

import java.util.UUID;

public interface MarkOrderPaidUseCase {
    void markPaid(OrderId orderId, UUID paymentId);
}
