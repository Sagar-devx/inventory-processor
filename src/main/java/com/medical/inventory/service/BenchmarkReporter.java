package com.medical.inventory.service;

import com.medical.inventory.dto.ProcessingReport;
import com.medical.inventory.entity.ProcessingError;
import com.medical.inventory.entity.ProcessingJob;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BenchmarkReporter {

    public BigDecimal calculateSpeedup(long sequentialDurationMs, long parallelDurationMs) {
        if (parallelDurationMs <= 0 || sequentialDurationMs <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(sequentialDurationMs)
                .divide(BigDecimal.valueOf(parallelDurationMs), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateThroughput(int totalRecords, long durationMs) {
        if (durationMs <= 0 || totalRecords <= 0) {
            return BigDecimal.ZERO;
        }
        double seconds = durationMs / 1000.0;
        return BigDecimal.valueOf(totalRecords / seconds)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public ProcessingReport generateReport(ProcessingJob job, List<ProcessingError> errors) {
        List<ProcessingReport.ErrorDetail> errorDetails = errors.stream()
                .limit(100) // return up to 100 recent errors in report
                .map(err -> new ProcessingReport.ErrorDetail(
                        err.getId(),
                        err.getRowNumber(),
                        err.getEntryCode(),
                        err.getErrorMessage(),
                        err.getRawData()
                )).toList();

        return new ProcessingReport(
                job.getId(),
                job.getFileName(),
                job.getFileSizeBytes(),
                job.getStatus() != null ? job.getStatus().name() : "UNKNOWN",
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
                job.getCreatedBy(),
                errors.size(),
                errorDetails
        );
    }
}
