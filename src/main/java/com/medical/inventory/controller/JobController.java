package com.medical.inventory.controller;

import com.medical.inventory.dto.ApiResponse;
import com.medical.inventory.dto.JobResponse;
import com.medical.inventory.dto.PagedResponse;
import com.medical.inventory.dto.ProcessingReport;
import com.medical.inventory.repository.ProcessingJobRepository;
import com.medical.inventory.service.CsvProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Processing Jobs", description = "Endpoints for inspecting processing jobs and benchmark reports")
@SecurityRequirement(name = "Bearer Authentication")
public class JobController {

    private final CsvProcessingService csvProcessingService;
    private final ProcessingJobRepository processingJobRepository;

    public JobController(CsvProcessingService csvProcessingService, ProcessingJobRepository processingJobRepository) {
        this.csvProcessingService = csvProcessingService;
        this.processingJobRepository = processingJobRepository;
    }

    @GetMapping
    @Operation(summary = "List all processing jobs (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<JobResponse>>> listJobs(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<JobResponse> page = processingJobRepository.findAll(pageable)
                .map(JobResponse::fromEntity);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(page)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full processing report and benchmark metrics for a job")
    public ResponseEntity<ApiResponse<ProcessingReport>> getJobReport(@PathVariable UUID id) {
        ProcessingReport report = csvProcessingService.getJobReport(id);
        return ResponseEntity.ok(ApiResponse.success(report, "Job report retrieved successfully"));
    }
}
