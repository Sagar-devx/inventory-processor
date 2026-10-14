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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ParallelProcessor {

    private static final Logger log = LoggerFactory.getLogger(ParallelProcessor.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int BATCH_SIZE = 500;
    private static final int MAX_CONCURRENT_WORKERS = 10;

    private final CsvRecordValidator validator;
    private final InventoryItemRepository inventoryItemRepository;

    public ParallelProcessor(CsvRecordValidator validator, InventoryItemRepository inventoryItemRepository) {
        this.validator = validator;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    public ProcessingResult process(InputStream inputStream, ProcessingJob job) {
        long startTime = System.currentTimeMillis();

        AtomicInteger totalCounter = new AtomicInteger(0);
        AtomicInteger validCounter = new AtomicInteger(0);
        AtomicInteger invalidCounter = new AtomicInteger(0);
        AtomicInteger duplicateCounter = new AtomicInteger(0);

        ConcurrentLinkedQueue<InventoryItem> validItems = new ConcurrentLinkedQueue<>();
        List<ProcessingError> errors = Collections.synchronizedList(new ArrayList<>());
        Set<String> seenKeys = ConcurrentHashMap.newKeySet();

        Semaphore semaphore = new Semaphore(MAX_CONCURRENT_WORKERS);

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        // Use Java 21 Virtual Threads Executor
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser parser = new CSVParser(reader, csvFormat)) {

            List<Future<?>> taskFutures = new ArrayList<>();
            List<CSVRecord> currentBatch = new ArrayList<>(BATCH_SIZE);

            for (CSVRecord record : parser) {
                totalCounter.incrementAndGet();
                currentBatch.add(record);

                if (currentBatch.size() >= BATCH_SIZE) {
                    List<CSVRecord> batchToProcess = new ArrayList<>(currentBatch);
                    currentBatch.clear();

                    semaphore.acquire();
                    Future<?> future = executor.submit(() -> {
                        try {
                            processBatch(batchToProcess, job, validCounter, invalidCounter, duplicateCounter,
                                    seenKeys, validItems, errors);
                        } finally {
                            semaphore.release();
                        }
                    });
                    taskFutures.add(future);
                }
            }

            // Process any remaining tail batch
            if (!currentBatch.isEmpty()) {
                List<CSVRecord> batchToProcess = new ArrayList<>(currentBatch);
                semaphore.acquire();
                Future<?> future = executor.submit(() -> {
                    try {
                        processBatch(batchToProcess, job, validCounter, invalidCounter, duplicateCounter,
                                seenKeys, validItems, errors);
                    } finally {
                        semaphore.release();
                    }
                });
                taskFutures.add(future);
            }

            // Await all batch tasks completion
            for (Future<?> f : taskFutures) {
                f.get();
            }

        } catch (Exception e) {
            log.error("Parallel CSV processing failed: {}", e.getMessage(), e);
            throw new RuntimeException("Parallel CSV processing error: " + e.getMessage(), e);
        }

        long duration = System.currentTimeMillis() - startTime;
        return new ProcessingResult(
                duration,
                totalCounter.get(),
                validCounter.get(),
                invalidCounter.get(),
                duplicateCounter.get(),
                new ArrayList<>(validItems),
                new ArrayList<>(errors)
        );
    }

    private void processBatch(List<CSVRecord> batch,
                              ProcessingJob job,
                              AtomicInteger validCounter,
                              AtomicInteger invalidCounter,
                              AtomicInteger duplicateCounter,
                              Set<String> seenKeys,
                              ConcurrentLinkedQueue<InventoryItem> validItems,
                              List<ProcessingError> errors) {

        for (CSVRecord record : batch) {
            int rowNumber = (int) record.getRecordNumber() + 1;
            ValidationResult valResult = validator.validate(record);

            if (!valResult.valid()) {
                invalidCounter.incrementAndGet();
                String entryCode = getSafe(record, "entry_code", 0);
                String errorMsg = String.join("; ", valResult.errors());
                errors.add(new ProcessingError(job, rowNumber, entryCode, errorMsg, record.toString()));
                continue;
            }

            String entryCode = getSafe(record, "entry_code", 0);
            String itemName = getSafe(record, "item_name", 1);
            String batchNumber = getSafe(record, "batch_number", 2);
            String compositeKey = entryCode.trim() + ":" + batchNumber.trim();

            // Thread-safe duplicate check
            if (!seenKeys.add(compositeKey) || inventoryItemRepository.existsByEntryCodeAndBatchNumber(entryCode, batchNumber)) {
                duplicateCounter.incrementAndGet();
                errors.add(new ProcessingError(job, rowNumber, entryCode,
                        "Duplicate record detected for key: " + compositeKey, record.toString()));
                continue;
            }

            try {
                int quantity = Integer.parseInt(getSafe(record, "quantity", 3));
                BigDecimal price = new BigDecimal(getSafe(record, "unit_price", 4));
                LocalDate expiry = LocalDate.parse(getSafe(record, "expiry_date", 5), DATE_FORMATTER);

                InventoryItem item = new InventoryItem(entryCode, itemName, batchNumber, quantity, price, expiry, InventoryStatus.ACTIVE);
                validItems.add(item);
                validCounter.incrementAndGet();
            } catch (Exception ex) {
                invalidCounter.incrementAndGet();
                errors.add(new ProcessingError(job, rowNumber, entryCode,
                        "Parsing error: " + ex.getMessage(), record.toString()));
            }
        }
    }

    private String getSafe(CSVRecord record, String name, int index) {
        if (record.isMapped(name)) return record.get(name);
        if (record.size() > index) return record.get(index);
        return "";
    }
}
