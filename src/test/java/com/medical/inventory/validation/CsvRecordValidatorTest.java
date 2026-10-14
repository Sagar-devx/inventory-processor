package com.medical.inventory.validation;

import com.medical.inventory.dto.CsvInventoryRecord;
import com.medical.inventory.dto.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvRecordValidatorTest {

    private CsvRecordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CsvRecordValidator();
    }

    @Test
    @DisplayName("Should pass validation for a valid inventory record")
    void testValidate_ValidRecord() {
        CsvInventoryRecord record = new CsvInventoryRecord(
                1, "MED-001", "Paracetamol 500mg", "BAT-001", 100,
                new BigDecimal("12.50"), LocalDate.of(2026, 12, 31), "ACTIVE", "raw"
        );

        ValidationResult result = validator.validate(record);
        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("Should reject record with missing entry code")
    void testValidate_MissingEntryCode() {
        CsvInventoryRecord record = new CsvInventoryRecord(
                2, "", "Paracetamol 500mg", "BAT-001", 100,
                new BigDecimal("12.50"), LocalDate.of(2026, 12, 31), "ACTIVE", "raw"
        );

        ValidationResult result = validator.validate(record);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Entry code is missing")));
    }

    @Test
    @DisplayName("Should reject record with negative quantity")
    void testValidate_NegativeQuantity() {
        CsvInventoryRecord record = new CsvInventoryRecord(
                3, "MED-001", "Paracetamol 500mg", "BAT-001", -10,
                new BigDecimal("12.50"), LocalDate.of(2026, 12, 31), "ACTIVE", "raw"
        );

        ValidationResult result = validator.validate(record);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Quantity must be greater than or equal to 0")));
    }

    @Test
    @DisplayName("Should reject record with zero or negative price")
    void testValidate_InvalidPrice() {
        CsvInventoryRecord record = new CsvInventoryRecord(
                4, "MED-001", "Paracetamol 500mg", "BAT-001", 100,
                new BigDecimal("-5.00"), LocalDate.of(2026, 12, 31), "ACTIVE", "raw"
        );

        ValidationResult result = validator.validate(record);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Unit price must be strictly greater than 0")));
    }

    @Test
    @DisplayName("Should reject record with missing expiry date")
    void testValidate_MissingExpiryDate() {
        CsvInventoryRecord record = new CsvInventoryRecord(
                5, "MED-001", "Paracetamol 500mg", "BAT-001", 100,
                new BigDecimal("15.00"), null, "ACTIVE", "raw"
        );

        ValidationResult result = validator.validate(record);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Expiry date is missing")));
    }
}
