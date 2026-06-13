package com.example.ecommerce.product.fakes;

import com.example.ecommerce.shared.event.IntegrationEvent;
import com.example.ecommerce.shared.port.EventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryEventPublisher implements EventPublisher {

    private final List<IntegrationEvent> published = new CopyOnWriteArrayList<>();

    @Override
    public void publish(IntegrationEvent event) {
        published.add(event);
    }

    public List<IntegrationEvent> published() {
        return new ArrayList<>(published);
    }

    public void clear() {
        published.clear();
    }
}
