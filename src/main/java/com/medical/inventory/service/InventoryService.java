package com.medical.inventory.service;

import com.medical.inventory.dto.InventoryRequest;
import com.medical.inventory.dto.InventoryResponse;
import com.medical.inventory.dto.PagedResponse;
import com.medical.inventory.entity.InventoryItem;
import com.medical.inventory.entity.InventoryStatus;
import com.medical.inventory.exception.DuplicateResourceException;
import com.medical.inventory.exception.ResourceNotFoundException;
import com.medical.inventory.repository.InventoryItemRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;

    public InventoryService(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Transactional
    public InventoryResponse createItem(InventoryRequest request) {
        validateBusinessRules(request.quantity(), request.unitPrice());

        if (inventoryItemRepository.existsByEntryCodeAndBatchNumber(request.entryCode(), request.batchNumber())) {
            throw new DuplicateResourceException("Inventory item with entryCode " + request.entryCode()
                    + " and batchNumber " + request.batchNumber() + " already exists.");
        }

        InventoryItem item = new InventoryItem(
                request.entryCode().trim(),
                request.itemName().trim(),
                request.batchNumber().trim(),
                request.quantity(),
                request.unitPrice(),
                request.expiryDate(),
                request.status() != null ? request.status() : InventoryStatus.ACTIVE
        );

        return InventoryResponse.fromEntity(inventoryItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public InventoryResponse getItemById(Long id) {
        return inventoryItemRepository.findById(id)
                .map(InventoryResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "id", id));
    }

    @Transactional(readOnly = true)
    public PagedResponse<InventoryResponse> getAllItems(Pageable pageable) {
        Page<InventoryResponse> page = inventoryItemRepository.findAll(pageable)
                .map(InventoryResponse::fromEntity);
        return PagedResponse.from(page);
    }

    @Transactional
    public InventoryResponse updateItem(Long id, InventoryRequest request) {
        validateBusinessRules(request.quantity(), request.unitPrice());

        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InventoryItem", "id", id));

        // If composite key is changing, check for collision
        if (!item.getEntryCode().equalsIgnoreCase(request.entryCode()) ||
                !item.getBatchNumber().equalsIgnoreCase(request.batchNumber())) {
            if (inventoryItemRepository.existsByEntryCodeAndBatchNumber(request.entryCode(), request.batchNumber())) {
                throw new DuplicateResourceException("Cannot update: entryCode " + request.entryCode()
                        + " and batchNumber " + request.batchNumber() + " already exists.");
            }
        }

        item.setEntryCode(request.entryCode().trim());
        item.setItemName(request.itemName().trim());
        item.setBatchNumber(request.batchNumber().trim());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setExpiryDate(request.expiryDate());
        if (request.status() != null) {
            item.setStatus(request.status());
        }

        return InventoryResponse.fromEntity(inventoryItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public PagedResponse<InventoryResponse> search(String name, String entryCode, String batchNumber, Pageable pageable) {
        Specification<InventoryItem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("itemName")), "%" + name.trim().toLowerCase() + "%"));
            }
            if (entryCode != null && !entryCode.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("entryCode")), entryCode.trim().toLowerCase()));
            }
            if (batchNumber != null && !batchNumber.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("batchNumber")), batchNumber.trim().toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<InventoryResponse> page = inventoryItemRepository.findAll(spec, pageable)
                .map(InventoryResponse::fromEntity);
        return PagedResponse.from(page);
    }

    private void validateBusinessRules(Integer quantity, BigDecimal unitPrice) {
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + quantity);
        }
        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be strictly greater than 0: " + unitPrice);
        }
    }
}
