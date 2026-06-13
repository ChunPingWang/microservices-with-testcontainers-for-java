package com.example.ecommerce.product.fakes;

import com.example.ecommerce.product.domain.model.Order;
import com.example.ecommerce.product.domain.model.OrderId;
import com.example.ecommerce.product.domain.port.outbound.OrderWriteRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderWriteRepository implements OrderWriteRepository {

    private final Map<OrderId, Order> store = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        store.put(order.id(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(store.get(id));
    }

    public int size() {
        return store.size();
    }

    public void clear() {
        store.clear();
    }
}
