package com.medical.inventory.service;

import com.medical.inventory.dto.ProcessingReport;
import com.medical.inventory.dto.ProcessingResult;
import com.medical.inventory.entity.InventoryItem;
import com.medical.inventory.entity.JobStatus;
import com.medical.inventory.entity.ProcessingError;
import com.medical.inventory.entity.ProcessingJob;
import com.medical.inventory.exception.ResourceNotFoundException;
import com.medical.inventory.repository.InventoryItemRepository;
import com.medical.inventory.repository.ProcessingErrorRepository;
import com.medical.inventory.repository.ProcessingJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CsvProcessingService {

    private static final Logger log = LoggerFactory.getLogger(CsvProcessingService.class);
    private static final int DB_BATCH_SIZE = 500;

    private final ProcessingJobRepository jobRepository;
    private final ProcessingErrorRepository errorRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SequentialProcessor sequentialProcessor;
    private final ParallelProcessor parallelProcessor;
    private final BenchmarkReporter benchmarkReporter;

    public CsvProcessingService(ProcessingJobRepository jobRepository,
                                ProcessingErrorRepository errorRepository,
                                InventoryItemRepository inventoryItemRepository,
                                SequentialProcessor sequentialProcessor,
                                ParallelProcessor parallelProcessor,
                                BenchmarkReporter benchmarkReporter) {
        this.jobRepository = jobRepository;
        this.errorRepository = errorRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.sequentialProcessor = sequentialProcessor;
        this.parallelProcessor = parallelProcessor;
        this.benchmarkReporter = benchmarkReporter;
    }

    @Transactional
    public ProcessingJob createJob(String fileName, long fileSize, String createdBy) {
        ProcessingJob job = new ProcessingJob(UUID.randomUUID(), fileName, fileSize, createdBy);
        return jobRepository.save(job);
    }

    @Async("taskExecutor")
    public void processFileAsync(Path tempFilePath, UUID jobId) {
        log.info("Starting asynchronous processing for job {}", jobId);

        ProcessingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ProcessingJob", "id", jobId));

        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);

        try {
            // 1. Run Sequential baseline
            log.info("Running sequential baseline for job {}", jobId);
            ProcessingResult seqResult;
            try (InputStream is = Files.newInputStream(tempFilePath)) {
                seqResult = sequentialProcessor.process(is, job);
            }

            // 2. Run Parallel processor (using Virtual Threads)
            log.info("Running parallel processor with virtual threads for job {}", jobId);
            ProcessingResult parResult;
            try (InputStream is = Files.newInputStream(tempFilePath)) {
                parResult = parallelProcessor.process(is, job);
            }

            // 3. Persist valid items in batches to PostgreSQL
            log.info("Persisting {} valid inventory items for job {}", parResult.validItems().size(), jobId);
            saveItemsInBatches(parResult.validItems());

            // 4. Persist processing errors
            log.info("Persisting {} error records for job {}", parResult.errors().size(), jobId);
            saveErrorsInBatches(parResult.errors());

            // 5. Calculate benchmark metrics
            BigDecimal speedup = benchmarkReporter.calculateSpeedup(seqResult.durationMs(), parResult.durationMs());
            BigDecimal throughput = benchmarkReporter.calculateThroughput(parResult.totalCount(), parResult.durationMs());

            // 6. Update and complete the job
            job.setTotalRecords(parResult.totalCount());
            job.setProcessedRecords(parResult.totalCount());
            job.setSuccessfulRecords(parResult.validCount());
            job.setFailedRecords(parResult.invalidCount());
            job.setDuplicateRecords(parResult.duplicateCount());
            job.setSequentialDurationMs(seqResult.durationMs());
            job.setParallelDurationMs(parResult.durationMs());
            job.setSpeedupFactor(speedup);
            job.setThroughputRecordsPerSec(throughput);
            job.setCompletedAt(Instant.now());
            job.setStatus(JobStatus.COMPLETED);

            jobRepository.save(job);
            log.info("Successfully completed job {}. Seq: {}ms, Par: {}ms, Speedup: {}x, Throughput: {} rec/s",
                    jobId, seqResult.durationMs(), parResult.durationMs(), speedup, throughput);

        } catch (Exception e) {
            log.error("Fatal error processing job {}: {}", jobId, e.getMessage(), e);
            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        } finally {
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (Exception ignored) {
            }
        }
    }

    @Transactional(readOnly = true)
    public ProcessingReport getJobReport(UUID jobId) {
        ProcessingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ProcessingJob", "id", jobId));
        List<ProcessingError> errors = errorRepository.findByJobId(jobId);
        return benchmarkReporter.generateReport(job, errors);
    }

    private void saveItemsInBatches(List<InventoryItem> items) {
        if (items == null || items.isEmpty()) return;
        List<InventoryItem> batch = new ArrayList<>(DB_BATCH_SIZE);
        for (InventoryItem item : items) {
            batch.add(item);
            if (batch.size() >= DB_BATCH_SIZE) {
                inventoryItemRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            inventoryItemRepository.saveAll(batch);
        }
    }

    private void saveErrorsInBatches(List<ProcessingError> errors) {
        if (errors == null || errors.isEmpty()) return;
        List<ProcessingError> batch = new ArrayList<>(DB_BATCH_SIZE);
        for (ProcessingError err : errors) {
            batch.add(err);
            if (batch.size() >= DB_BATCH_SIZE) {
                errorRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            errorRepository.saveAll(batch);
        }
    }
}
