package com.example.ecommerce.payment.config;

import com.example.ecommerce.payment.application.command.ProcessPaymentCommandHandler;
import com.example.ecommerce.payment.domain.port.inbound.ProcessPaymentUseCase;
import com.example.ecommerce.payment.domain.port.outbound.NotificationPort;
import com.example.ecommerce.payment.domain.port.outbound.PaymentGateway;
import com.example.ecommerce.payment.domain.port.outbound.PaymentRepository;
import com.example.ecommerce.payment.domain.port.outbound.ReceiptStoragePort;
import com.example.ecommerce.payment.domain.service.PaymentValidationService;
import com.example.ecommerce.shared.port.EventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PaymentValidationService paymentValidationService() {
        return new PaymentValidationService();
    }

    @Bean
    ProcessPaymentUseCase processPaymentUseCase(
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            ReceiptStoragePort receiptStorage,
            NotificationPort notification,
            EventPublisher eventPublisher,
            PaymentValidationService validation,
            Clock clock) {
        return new ProcessPaymentCommandHandler(
                paymentRepository, paymentGateway, receiptStorage,
                notification, eventPublisher, validation, clock);
    }
}
