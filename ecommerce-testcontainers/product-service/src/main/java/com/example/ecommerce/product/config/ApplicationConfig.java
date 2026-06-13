package com.example.ecommerce.product.config;

import com.example.ecommerce.product.application.command.MarkOrderPaidCommandHandler;
import com.example.ecommerce.product.application.command.PlaceOrderCommandHandler;
import com.example.ecommerce.product.application.query.SearchProductQueryHandler;
import com.example.ecommerce.product.domain.port.inbound.PlaceOrderUseCase;
import com.example.ecommerce.product.domain.port.inbound.SearchProductUseCase;
import com.example.ecommerce.product.domain.port.outbound.CachePort;
import com.example.ecommerce.product.domain.port.outbound.OrderWriteRepository;
import com.example.ecommerce.product.domain.port.outbound.SearchPort;
import com.example.ecommerce.product.domain.service.PricingService;
import com.example.ecommerce.shared.port.EventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Clock;

@Configuration
public class ApplicationConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PricingService pricingService(@Value("${pricing.tax-rate:0.05}") BigDecimal taxRate) {
        return new PricingService(taxRate);
    }

    @Bean
    PlaceOrderUseCase placeOrderUseCase(OrderWriteRepository repo,
                                        EventPublisher publisher,
                                        Clock clock) {
        return new PlaceOrderCommandHandler(repo, publisher, clock);
    }

    /**
     * Single bean exposes both inbound ports — registering it twice
     * causes duplicate-bean errors when Spring resolves by interface type.
     */
    @Bean
    MarkOrderPaidCommandHandler orderLifecycleHandler(OrderWriteRepository repo, Clock clock) {
        return new MarkOrderPaidCommandHandler(repo, clock);
    }

    @Bean
    SearchProductUseCase searchProductUseCase(SearchPort searchPort, CachePort cachePort) {
        return new SearchProductQueryHandler(searchPort, cachePort);
    }
}
