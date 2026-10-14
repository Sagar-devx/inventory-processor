package com.medical.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProcessingReport(
    UUID jobId,
    String fileName,
    Long fileSizeBytes,
    String status,
    int totalRecords,
    int processedRecords,
    int successfulRecords,
    int failedRecords,
    int duplicateRecords,
    long sequentialDurationMs,
    long parallelDurationMs,
    BigDecimal speedupFactor,
    BigDecimal throughputRecordsPerSec,
    Instant startedAt,
    Instant completedAt,
    String createdBy,
    int errorCount,
    List<ErrorDetail> recentErrors
) {
    public record ErrorDetail(
        Long id,
        int rowNumber,
        String entryCode,
        String errorMessage,
        String rawData
    ) {}
}
