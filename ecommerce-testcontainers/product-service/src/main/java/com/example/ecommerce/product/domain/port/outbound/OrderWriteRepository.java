package com.example.ecommerce.product.domain.port.outbound;

import com.example.ecommerce.product.domain.model.Order;
import com.example.ecommerce.product.domain.model.OrderId;

import java.util.Optional;

public interface OrderWriteRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
}
