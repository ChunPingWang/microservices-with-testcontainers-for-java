package com.example.ecommerce.payment.adapter.outbound.notification;

import com.example.ecommerce.payment.domain.port.outbound.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Default notification adapter used outside the "pubsub" profile — just logs. */
@Component
@Profile("!pubsub")
public class LogNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationAdapter.class);

    @Override
    public void notifyPaid(UUID orderId, String buyerId, String receiptUri) {
        log.info("[notify] paid order={} buyer={} receipt={}", orderId, buyerId, receiptUri);
    }

    @Override
    public void notifyFailed(UUID orderId, String buyerId, String reason) {
        log.warn("[notify] failed order={} buyer={} reason={}", orderId, buyerId, reason);
    }
}
