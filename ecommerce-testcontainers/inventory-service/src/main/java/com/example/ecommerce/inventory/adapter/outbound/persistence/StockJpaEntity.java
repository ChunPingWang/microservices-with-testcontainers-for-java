package com.example.ecommerce.inventory.adapter.outbound.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stocks")
class StockJpaEntity {

    @Id
    @Column(name = "sku", length = 64)
    private String sku;

    @Column(name = "available", nullable = false)
    private int available;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reservations", joinColumns = @JoinColumn(name = "sku"))
    private List<ReservationJpaEntity> reservations = new ArrayList<>();

    protected StockJpaEntity() {}

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public int getAvailable() { return available; }
    public void setAvailable(int available) { this.available = available; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public List<ReservationJpaEntity> getReservations() { return reservations; }
    public void setReservations(List<ReservationJpaEntity> reservations) { this.reservations = reservations; }
}
