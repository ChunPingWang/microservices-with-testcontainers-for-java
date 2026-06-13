package com.example.ecommerce.payment.contract;

import com.example.ecommerce.payment.domain.port.outbound.PaymentRepository;
import com.example.ecommerce.payment.fakes.InMemoryPaymentRepository;

/**
 * Contract test executed against the in-memory fake — runs in milliseconds,
 * proves the fake honours the {@link PaymentRepository} contract so unit
 * tests written with it are not lying.
 */
class InMemoryPaymentRepositoryContractTest extends PaymentRepositoryContract {

    private final InMemoryPaymentRepository repo = new InMemoryPaymentRepository();

    @Override
    protected PaymentRepository repository() {
        return repo;
    }
}
