package com.example.ecommerce.payment.domain.port.outbound;

import java.net.URI;

public interface ReceiptStoragePort {

    /** Returns a URI (e.g. s3://...) that identifies the stored receipt. */
    URI storeReceipt(String paymentId, byte[] pdfBytes);
}
