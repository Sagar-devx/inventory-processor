package com.medical.inventory.repository;

import com.medical.inventory.entity.ProcessingError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProcessingErrorRepository extends JpaRepository<ProcessingError, Long> {
    List<ProcessingError> findByJobId(UUID jobId);
    Page<ProcessingError> findByJobId(UUID jobId, Pageable pageable);
    long countByJobId(UUID jobId);
}
