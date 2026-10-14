package com.medical.inventory.controller;

import com.medical.inventory.dto.ApiResponse;
import com.medical.inventory.dto.InventoryRequest;
import com.medical.inventory.dto.InventoryResponse;
import com.medical.inventory.dto.PagedResponse;
import com.medical.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory Management", description = "Endpoints for searching, creating, and managing inventory records")
@SecurityRequirement(name = "Bearer Authentication")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "List all inventory items (paginated & sorted)")
    public ResponseEntity<ApiResponse<PagedResponse<InventoryResponse>>> getAllItems(
            @PageableDefault(size = 20, sort = "itemName", direction = Sort.Direction.ASC) Pageable pageable) {

        PagedResponse<InventoryResponse> response = inventoryService.getAllItems(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an inventory item by ID")
    public ResponseEntity<ApiResponse<InventoryResponse>> getItemById(@PathVariable Long id) {
        InventoryResponse response = inventoryService.getItemById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create an inventory item (ADMIN only)")
    public ResponseEntity<ApiResponse<InventoryResponse>> createItem(@Valid @RequestBody InventoryRequest request) {
        InventoryResponse response = inventoryService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Inventory item created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an inventory item (ADMIN only)")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody InventoryRequest request) {

        InventoryResponse response = inventoryService.updateItem(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Inventory item updated successfully"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search inventory by name, entry code, or batch number (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<InventoryResponse>>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String entryCode,
            @RequestParam(required = false) String batchNumber,
            @PageableDefault(size = 20, sort = "itemName", direction = Sort.Direction.ASC) Pageable pageable) {

        PagedResponse<InventoryResponse> response = inventoryService.search(name, entryCode, batchNumber, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Search results retrieved"));
    }
}
