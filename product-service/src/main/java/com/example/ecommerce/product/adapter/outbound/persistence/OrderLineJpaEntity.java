package com.example.ecommerce.product.adapter.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
class OrderLineJpaEntity {

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "unit_currency", nullable = false, length = 3)
    private String unitCurrency;

    protected OrderLineJpaEntity() {}

    OrderLineJpaEntity(String sku, int quantity, BigDecimal unitPrice, String unitCurrency) {
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.unitCurrency = unitCurrency;
    }

    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public String getUnitCurrency() { return unitCurrency; }
}
