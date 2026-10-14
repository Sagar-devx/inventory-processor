package com.medical.inventory.validation;

import com.medical.inventory.dto.CsvInventoryRecord;
import com.medical.inventory.dto.ValidationResult;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvRecordValidator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD

    public ValidationResult validate(CSVRecord record) {
        List<String> errors = new ArrayList<>();

        if (record == null) {
            return ValidationResult.failure("Record is null");
        }

        // Validate entry_code
        String entryCode = getColumnValue(record, "entry_code", 0);
        if (entryCode == null || entryCode.trim().isEmpty()) {
            errors.add("Entry code is missing or empty");
        } else if (entryCode.length() > 50) {
            errors.add("Entry code exceeds maximum length of 50 characters");
        }

        // Validate item_name
        String itemName = getColumnValue(record, "item_name", 1);
        if (itemName == null || itemName.trim().isEmpty()) {
            errors.add("Item name is missing or empty");
        } else if (itemName.length() > 255) {
            errors.add("Item name exceeds maximum length of 255 characters");
        }

        // Validate batch_number
        String batchNumber = getColumnValue(record, "batch_number", 2);
        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            errors.add("Batch number is missing or empty");
        } else if (batchNumber.length() > 50) {
            errors.add("Batch number exceeds maximum length of 50 characters");
        }

        // Validate quantity
        String quantityStr = getColumnValue(record, "quantity", 3);
        if (quantityStr == null || quantityStr.trim().isEmpty()) {
            errors.add("Quantity is missing or empty");
        } else {
            try {
                int quantity = Integer.parseInt(quantityStr.trim());
                if (quantity < 0) {
                    errors.add("Quantity must be greater than or equal to 0, received: " + quantity);
                }
            } catch (NumberFormatException e) {
                errors.add("Quantity must be a valid integer, received: " + quantityStr);
            }
        }

        // Validate unit_price
        String unitPriceStr = getColumnValue(record, "unit_price", 4);
        if (unitPriceStr == null || unitPriceStr.trim().isEmpty()) {
            errors.add("Unit price is missing or empty");
        } else {
            try {
                BigDecimal unitPrice = new BigDecimal(unitPriceStr.trim());
                if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Unit price must be strictly greater than 0, received: " + unitPriceStr);
                }
            } catch (NumberFormatException e) {
                errors.add("Unit price must be a valid decimal number, received: " + unitPriceStr);
            }
        }

        // Validate expiry_date
        String expiryDateStr = getColumnValue(record, "expiry_date", 5);
        if (expiryDateStr == null || expiryDateStr.trim().isEmpty()) {
            errors.add("Expiry date is missing or empty");
        } else {
            try {
                LocalDate.parse(expiryDateStr.trim(), DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                errors.add("Expiry date must be in ISO-8601 format (YYYY-MM-DD), received: " + expiryDateStr);
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    public ValidationResult validate(CsvInventoryRecord record) {
        List<String> errors = new ArrayList<>();

        if (record == null) {
            return ValidationResult.failure("Record is null");
        }

        if (record.entryCode() == null || record.entryCode().trim().isEmpty()) {
            errors.add("Entry code is missing or empty");
        } else if (record.entryCode().length() > 50) {
            errors.add("Entry code exceeds maximum length of 50 characters");
        }

        if (record.itemName() == null || record.itemName().trim().isEmpty()) {
            errors.add("Item name is missing or empty");
        } else if (record.itemName().length() > 255) {
            errors.add("Item name exceeds maximum length of 255 characters");
        }

        if (record.batchNumber() == null || record.batchNumber().trim().isEmpty()) {
            errors.add("Batch number is missing or empty");
        } else if (record.batchNumber().length() > 50) {
            errors.add("Batch number exceeds maximum length of 50 characters");
        }

        if (record.quantity() == null) {
            errors.add("Quantity is missing");
        } else if (record.quantity() < 0) {
            errors.add("Quantity must be greater than or equal to 0, received: " + record.quantity());
        }

        if (record.unitPrice() == null) {
            errors.add("Unit price is missing");
        } else if (record.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Unit price must be strictly greater than 0, received: " + record.unitPrice());
        }

        if (record.expiryDate() == null) {
            errors.add("Expiry date is missing");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    private String getColumnValue(CSVRecord record, String name, int defaultIndex) {
        if (record.isMapped(name)) {
            return record.get(name);
        }
        if (record.size() > defaultIndex) {
            return record.get(defaultIndex);
        }
        return null;
    }
}
