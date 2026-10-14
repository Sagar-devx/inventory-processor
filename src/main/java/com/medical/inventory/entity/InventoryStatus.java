package com.medical.inventory.entity;

public enum InventoryStatus {
    ACTIVE,
    EXPIRED,
    QUARANTINED,
    LOW_STOCK;

    public static InventoryStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ACTIVE;
        }
        try {
            return InventoryStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ACTIVE;
        }
    }
}
