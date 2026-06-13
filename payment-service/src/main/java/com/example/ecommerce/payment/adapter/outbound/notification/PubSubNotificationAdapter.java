package com.example.ecommerce.payment.adapter.outbound.notification;

import com.example.ecommerce.payment.domain.port.outbound.NotificationPort;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Sends customer notifications via Google Cloud Pub/Sub. Active when profile
 * "pubsub" is on; otherwise {@link LogNotificationAdapter} is used.
 */
@Component
@Profile("pubsub")
public class PubSubNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(PubSubNotificationAdapter.class);

    private final Publisher publisher;

    public PubSubNotificationAdapter(@Qualifier("notificationPublisher") Publisher publisher) {
        this.publisher = publisher;
    }

    @PreDestroy
    void close() {
        publisher.shutdown();
    }

    @Override
    public void notifyPaid(UUID orderId, String buyerId, String receiptUri) {
        send("payment.paid order=" + orderId + " buyer=" + buyerId + " receipt=" + receiptUri);
    }

    @Override
    public void notifyFailed(UUID orderId, String buyerId, String reason) {
        send("payment.failed order=" + orderId + " buyer=" + buyerId + " reason=" + reason);
    }

    private void send(String body) {
        PubsubMessage msg = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(body))
                .build();
        try {
            publisher.publish(msg).get();
        } catch (ApiException | InterruptedException | java.util.concurrent.ExecutionException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Pub/Sub publish failed (topic={}): {}", TopicName.parse(publisher.getTopicNameString()), e.toString());
        }
    }
}
