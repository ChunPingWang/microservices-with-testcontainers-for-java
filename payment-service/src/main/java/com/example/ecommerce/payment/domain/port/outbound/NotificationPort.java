package com.example.ecommerce.payment.domain.port.outbound;

import java.util.UUID;

public interface NotificationPort {

    void notifyPaid(UUID orderId, String buyerId, String receiptUri);

    void notifyFailed(UUID orderId, String buyerId, String reason);
}
