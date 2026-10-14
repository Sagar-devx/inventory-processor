package com.medical.inventory.dto;

import com.medical.inventory.entity.InventoryStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InventoryRequest(
    @NotBlank(message = "Entry code is required")
    @Size(max = 50, message = "Entry code must not exceed 50 characters")
    String entryCode,

    @NotBlank(message = "Item name is required")
    @Size(max = 255, message = "Item name must not exceed 255 characters")
    String itemName,

    @NotBlank(message = "Batch number is required")
    @Size(max = 50, message = "Batch number must not exceed 50 characters")
    String batchNumber,

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be greater than or equal to 0")
    Integer quantity,

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    BigDecimal unitPrice,

    @NotNull(message = "Expiry date is required")
    LocalDate expiryDate,

    InventoryStatus status
) {
}
