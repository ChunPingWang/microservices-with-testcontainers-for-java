package com.example.ecommerce.product.adapter.messaging;

import com.example.ecommerce.product.adapter.outbound.messaging.KafkaEventPublisher;
import com.example.ecommerce.shared.event.OrderCreatedIntegrationEvent;
import com.example.ecommerce.test.podman.PodmanCompatibility;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Tag("integration")
class KafkaEventPublisherIT {

    static KafkaContainer kafka;
    static KafkaTemplate<String, Object> template;
    static KafkaEventPublisher publisher;

    @BeforeAll
    static void startup() {
        PodmanCompatibility.apply();
        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));
        kafka.start();

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        var factory = new DefaultKafkaProducerFactory<String, Object>(props);
        template = new KafkaTemplate<>(factory);
        publisher = new KafkaEventPublisher(template);
    }

    @AfterAll
    static void shutdown() {
        if (template != null) template.destroy();
        if (kafka != null) kafka.stop();
    }

    @Test
    void publishes_to_topic_with_event_id_as_key() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderCreatedIntegrationEvent event = new OrderCreatedIntegrationEvent(
                eventId, Instant.now(), orderId, "buyer-1",
                new BigDecimal("100.00"), "USD",
                List.of(new OrderCreatedIntegrationEvent.Line("SKU-1", 2, new BigDecimal("50.00"))));

        publisher.publish(event);

        AtomicReference<ConsumerRecord<String, OrderCreatedIntegrationEvent>> received = new AtomicReference<>();
        try (KafkaConsumer<String, OrderCreatedIntegrationEvent> consumer = newConsumer()) {
            consumer.subscribe(List.of(OrderCreatedIntegrationEvent.TOPIC));
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                ConsumerRecords<String, OrderCreatedIntegrationEvent> records = consumer.poll(Duration.ofMillis(500));
                if (!records.isEmpty()) {
                    received.set(records.iterator().next());
                }
                assertThat(received.get()).isNotNull();
            });
        }

        var record = received.get();
        assertThat(record.key()).isEqualTo(eventId.toString());
        assertThat(record.value().orderId()).isEqualTo(orderId);
        assertThat(record.value().buyerId()).isEqualTo("buyer-1");
        assertThat(record.value().lines()).hasSize(1);
    }

    private static KafkaConsumer<String, OrderCreatedIntegrationEvent> newConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.ecommerce.shared.event");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderCreatedIntegrationEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new KafkaConsumer<>(props);
    }
}
