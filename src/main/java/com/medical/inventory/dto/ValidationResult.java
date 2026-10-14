package com.medical.inventory.dto;

import java.util.List;

public record ValidationResult(
    boolean valid,
    List<String> errors
) {
    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors != null ? List.copyOf(errors) : List.of());
    }

    public static ValidationResult failure(String error) {
        return new ValidationResult(false, List.of(error));
    }
}
