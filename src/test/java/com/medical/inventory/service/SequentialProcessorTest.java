package com.medical.inventory.service;

import com.medical.inventory.dto.ProcessingResult;
import com.medical.inventory.entity.ProcessingJob;
import com.medical.inventory.repository.InventoryItemRepository;
import com.medical.inventory.validation.CsvRecordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SequentialProcessorTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    private SequentialProcessor processor;

    @BeforeEach
    void setUp() {
        CsvRecordValidator validator = new CsvRecordValidator();
        processor = new SequentialProcessor(validator, inventoryItemRepository);
    }

    @Test
    @DisplayName("Should sequentially process CSV records and separate valid, invalid, and duplicate records")
    void testProcess_Sequential() {
        String csvData = """
                entry_code,item_name,batch_number,quantity,unit_price,expiry_date
                MED-001,Paracetamol,BAT-001,100,10.50,2026-12-31
                MED-002,Amoxicillin,BAT-002,50,25.00,2026-11-30
                MED-001,Paracetamol,BAT-001,100,10.50,2026-12-31
                MED-003,InvalidItem,BAT-003,-5,0.00,2026-10-31
                """;

        when(inventoryItemRepository.existsByEntryCodeAndBatchNumber(anyString(), anyString()))
                .thenReturn(false);

        InputStream is = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        ProcessingJob job = new ProcessingJob(UUID.randomUUID(), "test.csv", 100L, "test");

        ProcessingResult result = processor.process(is, job);

        assertEquals(4, result.totalCount());
        assertEquals(2, result.validCount());
        assertEquals(1, result.duplicateCount()); // 3rd line is duplicate of 1st
        assertEquals(1, result.invalidCount());   // 4th line has negative quantity and zero price
        assertTrue(result.durationMs() >= 0);
    }
}
