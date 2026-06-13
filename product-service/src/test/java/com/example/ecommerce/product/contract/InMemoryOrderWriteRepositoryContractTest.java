package com.example.ecommerce.product.contract;

import com.example.ecommerce.product.domain.port.outbound.OrderWriteRepository;
import com.example.ecommerce.product.fakes.InMemoryOrderWriteRepository;

class InMemoryOrderWriteRepositoryContractTest extends OrderWriteRepositoryContract {

    private final InMemoryOrderWriteRepository repo = new InMemoryOrderWriteRepository();

    @Override
    protected OrderWriteRepository repository() {
        return repo;
    }
}
