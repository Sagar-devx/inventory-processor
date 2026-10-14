package com.medical.inventory.dto;

import com.medical.inventory.entity.InventoryItem;
import com.medical.inventory.entity.ProcessingError;

import java.util.List;

public record ProcessingResult(
    long durationMs,
    int totalCount,
    int validCount,
    int invalidCount,
    int duplicateCount,
    List<InventoryItem> validItems,
    List<ProcessingError> errors
) {
    public static ProcessingResult empty() {
        return new ProcessingResult(0L, 0, 0, 0, 0, List.of(), List.of());
    }
}
