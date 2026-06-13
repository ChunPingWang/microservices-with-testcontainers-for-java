package com.example.ecommerce.inventory.config;

import com.example.ecommerce.inventory.application.command.DeductInventoryCommandHandler;
import com.example.ecommerce.inventory.application.query.StockQueryHandler;
import com.example.ecommerce.inventory.domain.port.inbound.DeductInventoryUseCase;
import com.example.ecommerce.inventory.domain.port.inbound.QueryStockUseCase;
import com.example.ecommerce.inventory.domain.port.outbound.DistributedLockPort;
import com.example.ecommerce.inventory.domain.port.outbound.StockRepository;
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
    DeductInventoryUseCase deductInventoryUseCase(
            StockRepository stockRepository,
            DistributedLockPort lock,
            EventPublisher eventPublisher,
            Clock clock) {
        return new DeductInventoryCommandHandler(stockRepository, lock, eventPublisher, clock);
    }

    @Bean
    QueryStockUseCase queryStockUseCase(StockRepository stockRepository) {
        return new StockQueryHandler(stockRepository);
    }
}
