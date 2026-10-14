package com.medical.inventory.service;

import com.medical.inventory.dto.ProcessingResult;
import com.medical.inventory.dto.ValidationResult;
import com.medical.inventory.entity.InventoryItem;
import com.medical.inventory.entity.InventoryStatus;
import com.medical.inventory.entity.ProcessingError;
import com.medical.inventory.entity.ProcessingJob;
import com.medical.inventory.repository.InventoryItemRepository;
import com.medical.inventory.validation.CsvRecordValidator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SequentialProcessor {

    private static final Logger log = LoggerFactory.getLogger(SequentialProcessor.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final CsvRecordValidator validator;
    private final InventoryItemRepository inventoryItemRepository;

    public SequentialProcessor(CsvRecordValidator validator, InventoryItemRepository inventoryItemRepository) {
        this.validator = validator;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    public ProcessingResult process(InputStream inputStream, ProcessingJob job) {
        long startTime = System.currentTimeMillis();
        int total = 0;
        int valid = 0;
        int invalid = 0;
        int duplicate = 0;

        List<InventoryItem> validItems = new ArrayList<>();
        List<ProcessingError> errors = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser parser = new CSVParser(reader, csvFormat)) {

            for (CSVRecord record : parser) {
                total++;
                int rowNumber = (int) record.getRecordNumber() + 1; // 1-based data row including header

                ValidationResult valResult = validator.validate(record);
                if (!valResult.valid()) {
                    invalid++;
                    String entryCode = getSafe(record, "entry_code", 0);
                    String raw = record.toString();
                    String errorMsg = String.join("; ", valResult.errors());
                    errors.add(new ProcessingError(job, rowNumber, entryCode, errorMsg, raw));
                    continue;
                }

                String entryCode = getSafe(record, "entry_code", 0);
                String itemName = getSafe(record, "item_name", 1);
                String batchNumber = getSafe(record, "batch_number", 2);
                String compositeKey = entryCode.trim() + ":" + batchNumber.trim();

                // Duplicate check within batch or against database
                if (seenKeys.contains(compositeKey) || inventoryItemRepository.existsByEntryCodeAndBatchNumber(entryCode, batchNumber)) {
                    duplicate++;
                    errors.add(new ProcessingError(job, rowNumber, entryCode,
                            "Duplicate record detected for key: " + compositeKey, record.toString()));
                    continue;
                }

                seenKeys.add(compositeKey);

                try {
                    int quantity = Integer.parseInt(getSafe(record, "quantity", 3));
                    BigDecimal price = new BigDecimal(getSafe(record, "unit_price", 4));
                    LocalDate expiry = LocalDate.parse(getSafe(record, "expiry_date", 5), DATE_FORMATTER);

                    InventoryItem item = new InventoryItem(entryCode, itemName, batchNumber, quantity, price, expiry, InventoryStatus.ACTIVE);
                    validItems.add(item);
                    valid++;
                } catch (Exception ex) {
                    invalid++;
                    errors.add(new ProcessingError(job, rowNumber, entryCode,
                            "Parsing error: " + ex.getMessage(), record.toString()));
                }
            }
        } catch (Exception e) {
            log.error("Sequential CSV processing failed: {}", e.getMessage(), e);
            throw new RuntimeException("Sequential CSV processing error: " + e.getMessage(), e);
        }

        long duration = System.currentTimeMillis() - startTime;
        return new ProcessingResult(duration, total, valid, invalid, duplicate, validItems, errors);
    }

    private String getSafe(CSVRecord record, String name, int index) {
        if (record.isMapped(name)) return record.get(name);
        if (record.size() > index) return record.get(index);
        return "";
    }
}
