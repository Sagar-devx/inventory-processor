package com.medical.inventory.dto;

import com.medical.inventory.entity.InventoryItem;
import com.medical.inventory.entity.InventoryStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record InventoryResponse(
    Long id,
    String entryCode,
    String itemName,
    String batchNumber,
    Integer quantity,
    BigDecimal unitPrice,
    LocalDate expiryDate,
    InventoryStatus status,
    Instant createdAt,
    Instant updatedAt
) {
    public static InventoryResponse fromEntity(InventoryItem item) {
        if (item == null) {
            return null;
        }
        return new InventoryResponse(
            item.getId(),
            item.getEntryCode(),
            item.getItemName(),
            item.getBatchNumber(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getExpiryDate(),
            item.getStatus(),
            item.getCreatedAt(),
            item.getUpdatedAt()
        );
    }
}
