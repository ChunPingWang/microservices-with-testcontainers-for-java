package com.example.ecommerce.payment.domain.model;

import java.time.Instant;

public sealed interface PaymentStatus
        permits PaymentStatus.Initiated,
                PaymentStatus.Authorised,
                PaymentStatus.Completed,
                PaymentStatus.Failed,
                PaymentStatus.Refunded {

    Instant at();

    record Initiated(Instant at) implements PaymentStatus {}
    record Authorised(Instant at, String authCode) implements PaymentStatus {}
    record Completed(Instant at, String receiptUri) implements PaymentStatus {}
    record Failed(Instant at, String reason) implements PaymentStatus {}
    record Refunded(Instant at, String reason) implements PaymentStatus {}
}
