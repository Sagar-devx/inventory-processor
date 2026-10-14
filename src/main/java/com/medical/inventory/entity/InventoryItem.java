package com.medical.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(
    name = "inventory_items",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_entry_batch", columnNames = {"entry_code", "batch_number"})
    },
    indexes = {
        @Index(name = "idx_inv_item_name", columnList = "item_name"),
        @Index(name = "idx_inv_entry_code", columnList = "entry_code"),
        @Index(name = "idx_inv_batch_num", columnList = "batch_number"),
        @Index(name = "idx_inv_status", columnList = "status"),
        @Index(name = "idx_inv_expiry_date", columnList = "expiry_date")
    }
)
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_code", nullable = false, length = 50)
    private String entryCode;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "batch_number", nullable = false, length = 50)
    private String batchNumber;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InventoryStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public InventoryItem() {
    }

    public InventoryItem(String entryCode, String itemName, String batchNumber,
                         Integer quantity, BigDecimal unitPrice, LocalDate expiryDate,
                         InventoryStatus status) {
        this.entryCode = entryCode;
        this.itemName = itemName;
        this.batchNumber = batchNumber;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.expiryDate = expiryDate;
        this.status = status != null ? status : InventoryStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = InventoryStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntryCode() {
        return entryCode;
    }

    public void setEntryCode(String entryCode) {
        this.entryCode = entryCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryItem that = (InventoryItem) o;
        return Objects.equals(entryCode, that.entryCode) && Objects.equals(batchNumber, that.batchNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryCode, batchNumber);
    }

    @Override
    public String toString() {
        return "InventoryItem{" +
                "id=" + id +
                ", entryCode='" + entryCode + '\'' +
                ", itemName='" + itemName + '\'' +
                ", batchNumber='" + batchNumber + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", expiryDate=" + expiryDate +
                ", status=" + status +
                '}';
    }
}
