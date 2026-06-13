package com.example.ecommerce.product.fakes;

import com.example.ecommerce.product.domain.port.outbound.CachePort;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCachePort implements CachePort {

    private final Map<String, Object> store = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = store.get(key);
        if (value == null) return Optional.empty();
        if (!type.isInstance(value)) {
            throw new ClassCastException("cached value at " + key + " is " + value.getClass()
                    + " not " + type);
        }
        return Optional.of((T) value);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        store.put(key, value);
    }

    @Override
    public void invalidate(String key) {
        store.remove(key);
    }

    public void clear() {
        store.clear();
    }
}
