package com.medical.inventory.dto;

import com.medical.inventory.entity.JobStatus;
import com.medical.inventory.entity.ProcessingJob;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record JobResponse(
    UUID jobId,
    String fileName,
    Long fileSizeBytes,
    JobStatus status,
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
    Instant createdAt,
    String createdBy
) {
    public static JobResponse fromEntity(ProcessingJob job) {
        if (job == null) {
            return null;
        }
        return new JobResponse(
            job.getId(),
            job.getFileName(),
            job.getFileSizeBytes(),
            job.getStatus(),
            job.getTotalRecords() != null ? job.getTotalRecords() : 0,
            job.getProcessedRecords() != null ? job.getProcessedRecords() : 0,
            job.getSuccessfulRecords() != null ? job.getSuccessfulRecords() : 0,
            job.getFailedRecords() != null ? job.getFailedRecords() : 0,
            job.getDuplicateRecords() != null ? job.getDuplicateRecords() : 0,
            job.getSequentialDurationMs() != null ? job.getSequentialDurationMs() : 0L,
            job.getParallelDurationMs() != null ? job.getParallelDurationMs() : 0L,
            job.getSpeedupFactor() != null ? job.getSpeedupFactor() : BigDecimal.ZERO,
            job.getThroughputRecordsPerSec() != null ? job.getThroughputRecordsPerSec() : BigDecimal.ZERO,
            job.getStartedAt(),
            job.getCompletedAt(),
            job.getCreatedAt(),
            job.getCreatedBy()
        );
    }
}
