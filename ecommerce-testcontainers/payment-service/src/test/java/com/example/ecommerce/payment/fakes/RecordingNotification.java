package com.example.ecommerce.payment.fakes;

import com.example.ecommerce.payment.domain.port.outbound.NotificationPort;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingNotification implements NotificationPort {

    public record Paid(UUID orderId, String buyerId, String receiptUri) {}
    public record Failed(UUID orderId, String buyerId, String reason) {}

    public final List<Paid> paid = new CopyOnWriteArrayList<>();
    public final List<Failed> failed = new CopyOnWriteArrayList<>();

    @Override
    public void notifyPaid(UUID orderId, String buyerId, String receiptUri) {
        paid.add(new Paid(orderId, buyerId, receiptUri));
    }

    @Override
    public void notifyFailed(UUID orderId, String buyerId, String reason) {
        failed.add(new Failed(orderId, buyerId, reason));
    }

    public List<Paid> paidEvents() { return new ArrayList<>(paid); }
    public List<Failed> failedEvents() { return new ArrayList<>(failed); }
}
