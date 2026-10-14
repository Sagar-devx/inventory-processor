package com.medical.inventory.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "processing_jobs")
public class ProcessingJob {

    @Id
    private UUID id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobStatus status;

    @Column(name = "total_records")
    private Integer totalRecords = 0;

    @Column(name = "processed_records")
    private Integer processedRecords = 0;

    @Column(name = "successful_records")
    private Integer successfulRecords = 0;

    @Column(name = "failed_records")
    private Integer failedRecords = 0;

    @Column(name = "duplicate_records")
    private Integer duplicateRecords = 0;

    @Column(name = "sequential_duration_ms")
    private Long sequentialDurationMs = 0L;

    @Column(name = "parallel_duration_ms")
    private Long parallelDurationMs = 0L;

    @Column(name = "speedup_factor", precision = 6, scale = 2)
    private BigDecimal speedupFactor = BigDecimal.ZERO;

    @Column(name = "throughput_records_per_sec", precision = 10, scale = 2)
    private BigDecimal throughputRecordsPerSec = BigDecimal.ZERO;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProcessingError> errors = new ArrayList<>();

    public ProcessingJob() {
    }

    public ProcessingJob(UUID id, String fileName, Long fileSizeBytes, String createdBy) {
        this.id = id != null ? id : UUID.randomUUID();
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.createdBy = createdBy;
        this.status = JobStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = JobStatus.PENDING;
        }
    }

    public void addError(ProcessingError error) {
        errors.add(error);
        error.setJob(this);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Integer getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(Integer processedRecords) {
        this.processedRecords = processedRecords;
    }

    public Integer getSuccessfulRecords() {
        return successfulRecords;
    }

    public void setSuccessfulRecords(Integer successfulRecords) {
        this.successfulRecords = successfulRecords;
    }

    public Integer getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(Integer failedRecords) {
        this.failedRecords = failedRecords;
    }

    public Integer getDuplicateRecords() {
        return duplicateRecords;
    }

    public void setDuplicateRecords(Integer duplicateRecords) {
        this.duplicateRecords = duplicateRecords;
    }

    public Long getSequentialDurationMs() {
        return sequentialDurationMs;
    }

    public void setSequentialDurationMs(Long sequentialDurationMs) {
        this.sequentialDurationMs = sequentialDurationMs;
    }

    public Long getParallelDurationMs() {
        return parallelDurationMs;
    }

    public void setParallelDurationMs(Long parallelDurationMs) {
        this.parallelDurationMs = parallelDurationMs;
    }

    public BigDecimal getSpeedupFactor() {
        return speedupFactor;
    }

    public void setSpeedupFactor(BigDecimal speedupFactor) {
        this.speedupFactor = speedupFactor;
    }

    public BigDecimal getThroughputRecordsPerSec() {
        return throughputRecordsPerSec;
    }

    public void setThroughputRecordsPerSec(BigDecimal throughputRecordsPerSec) {
        this.throughputRecordsPerSec = throughputRecordsPerSec;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public List<ProcessingError> getErrors() {
        return errors;
    }

    public void setErrors(List<ProcessingError> errors) {
        this.errors = errors;
    }
}
