package com.medical.inventory.repository;

import com.medical.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long>, JpaSpecificationExecutor<InventoryItem> {

    Optional<InventoryItem> findByEntryCodeAndBatchNumber(String entryCode, String batchNumber);

    boolean existsByEntryCodeAndBatchNumber(String entryCode, String batchNumber);

    @Query("SELECT CONCAT(i.entryCode, ':', i.batchNumber) FROM InventoryItem i WHERE i.entryCode IN :entryCodes")
    Set<String> findExistingCompositeKeysByEntryCodes(@Param("entryCodes") Set<String> entryCodes);
}
