package com.medical.inventory.controller;

import com.medical.inventory.dto.ApiResponse;
import com.medical.inventory.entity.ProcessingJob;
import com.medical.inventory.service.CsvProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@Tag(name = "CSV Upload", description = "Endpoints for streaming CSV uploads and triggering background processing")
@SecurityRequirement(name = "Bearer Authentication")
public class UploadController {

    private final CsvProcessingService csvProcessingService;

    public UploadController(CsvProcessingService csvProcessingService) {
        this.csvProcessingService = csvProcessingService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload CSV file for concurrent processing (10,000+ records supported)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadCsv(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is missing or empty"));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only CSV files (.csv) are accepted"));
        }

        // Stream file directly to a temporary file to avoid keeping large 50MB files in RAM
        Path tempFile = Files.createTempFile("inv_upload_", ".csv");
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        String username = authentication != null ? authentication.getName() : "system";
        ProcessingJob job = csvProcessingService.createJob(
                originalFilename != null ? originalFilename : "inventory.csv",
                file.getSize(),
                username
        );

        // Fire and forget async processing
        csvProcessingService.processFileAsync(tempFile, job.getId());

        Map<String, Object> responseData = Map.of(
                "jobId", job.getId(),
                "fileName", job.getFileName(),
                "fileSizeBytes", job.getFileSizeBytes(),
                "status", job.getStatus().name(),
                "message", "File upload accepted. Asynchronous processing has started. Check status at /api/jobs/" + job.getId()
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(responseData, "Upload accepted for processing"));
    }
}
