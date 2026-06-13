package com.example.ecommerce.payment.fakes;

import com.example.ecommerce.payment.domain.port.outbound.ReceiptStoragePort;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryReceiptStorage implements ReceiptStoragePort {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public URI storeReceipt(String paymentId, byte[] pdfBytes) {
        store.put(paymentId, pdfBytes);
        return URI.create("mem://receipts/" + paymentId);
    }

    public byte[] get(String paymentId) {
        return store.get(paymentId);
    }
}
