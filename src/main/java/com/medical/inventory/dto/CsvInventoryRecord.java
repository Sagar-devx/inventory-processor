package com.medical.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CsvInventoryRecord(
    int rowNumber,
    String entryCode,
    String itemName,
    String batchNumber,
    Integer quantity,
    BigDecimal unitPrice,
    LocalDate expiryDate,
    String status,
    String rawData
) {
    public String getCompositeKey() {
        return (entryCode != null ? entryCode.trim() : "") + ":" + (batchNumber != null ? batchNumber.trim() : "");
    }
}
